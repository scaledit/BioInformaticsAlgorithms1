package com.ntoggle.kubitschek.infra

import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.http.scaladsl.server.directives.MarshallingDirectives
import akka.stream.Materializer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json._
import scalaz.{\/-, -\/, \/}

object PlayJsonSupportExt extends PlayJsonSupportExt
trait PlayJsonSupportExt extends PlayJsonSupport {

  implicit def rejectingPlayJsonUnmarshaller[A](
    implicit reads: Reads[A], mat: Materializer): FromEntityUnmarshaller[Rejection \/ A] =
    playJsValueUnmarshaller.map(js =>
      reads.reads(js) match {
        case JsSuccess(a, _) => \/-(a)
        case e: JsError => -\/(PlayJsonRejection(e))
      })

  override implicit def playJsonUnmarshaller[A](implicit reads: Reads[A], mat: Materializer): FromEntityUnmarshaller[A] = {
    def read(json: JsValue) = reads.reads(json).recoverTotal{ error =>
      val flatForm = CustomErrorHandlers.flattenJsError(error)
      sys.error(Json.toJson(flatForm).toString())
    }
    playJsValueUnmarshaller.map(read)
  }

  def validateJson[A](implicit reads: Reads[A], mat: Materializer): Directive1[A] = {
    import MarshallingDirectives._
    ValidatedDirectives.validate[Rejection, A](as[Rejection \/ A])
  }
}
