package com.ntoggle.kubitschek.infra

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.PathMatcher.{Unmatched, Matched}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{FutureDirectives, BasicDirectives, RouteDirectives}
import akka.http.scaladsl.unmarshalling._
import com.ntoggle.kubitschek.api._
import scala.util.{Try, Failure, Success}
import scalaz.\/
import akka.http.scaladsl.util.FastFuture._

object ValidatedDirectives extends ValidatedDirectives
trait ValidatedDirectives {
  import RouteDirectives._
  import BasicDirectives._
  import FutureDirectives._

  private def nullAsEmpty(value: String) = if (value == null) "" else value

  def pathValue[A: ParamExtractor]: PathMatcher1[A] =
    new PathMatcher1[A] {
      def apply(path: Path) = path match {
        case Path.Segment(segment, tail) =>
          ParamExtractor[A].extract(segment).fold(
            e => throw new RejectionError(PathExtractorRejection(e)),
            s => Matched(tail, Tuple1(s)))
        case _ => Unmatched
      }
    }

  /**
   * Uses disjunction instead of exceptions to allow Unmarshaller to return typed Rejection
   */
  def validate[E <: Rejection, A](um: FromRequestUnmarshaller[E \/ A]): Directive1[A] =
  // Mostly lifted from MarshallingDirectives.entity
    extractRequestContext.flatMap[Tuple1[A]] { ctx ⇒
      import ctx.executionContext
      onComplete(um(ctx.request)) flatMap {
        case Success(value) =>
          value.fold(e => reject(e), provide)
        case Failure(Unmarshaller.NoContentException) => reject(RequestEntityExpectedRejection)
        case Failure(Unmarshaller.UnsupportedContentTypeException(x)) => reject(UnsupportedRequestContentTypeRejection(x))
        case Failure(x: IllegalArgumentException) => reject(ValidationRejection(nullAsEmpty(x.getMessage), Some(x)))
        case Failure(x) => reject(MalformedRequestContentRejection(nullAsEmpty(x.getMessage), Option(x.getCause)))
      }
    } & cancelRejections(RequestEntityExpectedRejection.getClass, classOf[UnsupportedRequestContentTypeRejection])

  /**
   * This is pretty shitty attempt at reproducing behavior of onComplete in FutureDirectives
   * Fix me
   */
  def onApiResponseFutureComplete[T](future: => ApiResponseFuture[T]): Directive1[Try[T]] =
    Directive { inner => ctx =>
      import ctx.executionContext
      future.run.fast.transformWith(t =>
        inner(Tuple1(t.flatMap(
          _.fold(
            e => Failure(RejectionError(e)),
            s => Success(s)
          ))))(ctx))
    }
}
