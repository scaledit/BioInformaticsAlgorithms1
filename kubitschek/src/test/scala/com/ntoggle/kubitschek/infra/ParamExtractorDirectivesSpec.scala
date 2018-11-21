package com.ntoggle.kubitschek
package infra

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.RouteTest
import akka.http.specs2.Specs2Interface
import com.ntoggle.kubitschek.infra.CustomErrorHandlers._
import org.scalacheck.{Prop, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import play.api.libs.json.{Format, Json}
import scalaz.syntax.monad._
import scalaz.syntax.std.option._


class ParamExtractorDirectivesSpec extends Specification with RouteTest with Specs2Interface with ScalaCheck {
  import com.ntoggle.kubitschek.infra.ParamExtractorDirectives._
  import com.ntoggle.kubitschek.api.ApiParamExtractors._
  import PlayJsonSupportExt._

  def testRoutes(
    test: Route): server.Route =
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        rejectEmptyResponse {
          pathPrefix("test") {
            test
          }
        }
      }
    }

  val testURI = Uri("/test")

  case class FOO(value: Int)
  object FOO {
    implicit val formatFOO: Format[FOO] =
      Json.format[FOO]
  }

  case class BAR(value: String)
  object BAR {
    implicit val formatBAR: Format[BAR] =
      Json.format[BAR]
  }

  case class FOOBARrequest(
    foo: FOO,
    bar: BAR,
    st: String,
    n: Int)

  object FOOBARrequest {
    implicit val formatFOOBARrequest: Format[FOOBARrequest] =
      Json.format[FOOBARrequest]
  }

  implicit val peFOO: ParamExtractor[FOO] =
    ParamExtractor[Int].map(FOO.apply)
  implicit val peBAR: ParamExtractor[BAR] =
    ParamExtractor[String].map(BAR.apply)


  "ParamExtractorDirectives" should {

    "queryParamMap" in {
      val route: Route = (get & queryParamMap) {
        complete(_)
      }
      val uri = testURI.withQuery(
        "param1" -> "value1",
        "param2" -> "value2",
        "param3" -> "value3")
      Get(uri) ~> testRoutes(route) ~> check {
        responseAs[Map[String, String]] ==== Map(
          "param1" -> "value1",
          "param2" -> "value2",
          "param3" -> "value3")
      }
    }

    "repeated queryparam" in {
      val route: Route = (get & queryparam("param".as[FOO].*)) {
        complete(_)
      }
      val uri = testURI.withQuery(
        "param" -> "1",
        "param" -> "2",
        "param" -> "3")
      Get(uri) ~> testRoutes(route) ~> check {
        responseAs[List[FOO]] ==== List(FOO(3), FOO(2), FOO(1))
      }
    }

    "queryParam for string" in Prop.forAll(Gen.alphaStr) { (s: String) =>
      val route: Route = (get & queryparam("param".as[String])) {
        complete(_)
      }
      val uri = testURI.withQuery("param" -> s)
      Get(uri) ~> testRoutes(route) ~> check {
        responseAs[String] ==== s
      }
    }

    "queryParam for Int" in Prop.forAll { (n: Int) =>
      val route: Route = (get & queryparam("param".as[Int])) {
        complete(_)
      }
      val uri = testURI.withQuery("param" -> n.toString)
      Get(uri) ~> testRoutes(route) ~> check {
        responseAs[Int] ==== n
      }
    }

    "queryParam for FOO" in Prop.forAll { (n: Int) =>
      val route: Route = (get & queryparam("param".as[FOO])) {
        complete(_)
      }
      val uri = testURI.withQuery("param" -> n.toString)
      Get(uri) ~> testRoutes(route) ~> check {
        responseAs[FOO].value ==== n
      }
    }

    "queryParam for BAR" in Prop.forAll(Gen.alphaStr) { (s: String) =>
      val route: Route = (get & queryparam("param".as[BAR])) {
        complete(_)
      }
      val uri = testURI.withQuery("param" -> s)
      Get(uri) ~> testRoutes(route) ~> check {
        responseAs[BAR].value ==== s
      }
    }

    "queryParam for ? string" in Prop.forAll(Gen.alphaStr) { (s: String) =>
      val route: Route = (get & queryparam("param".as[String].?)) {
        complete(_)
      }
      val uri = testURI.withQuery("param" -> s)
      Get(uri) ~> testRoutes(route) ~> check {
        responseAs[Option[String]] ==== Some(s)
      }
      Get(testURI) ~> testRoutes(route) ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "The requested resource could not be found."
      }
    }

    "queryParam for ? string with default" in Prop.forAll(Gen.alphaStr, Gen.alphaStr) {
      (s: String, default: String) =>
        val route: Route = (get & queryparam("param".?[String](default))) {
          complete(_)
        }
        val uri = testURI.withQuery("param" -> s)
        Get(uri) ~> testRoutes(route) ~> check {
          responseAs[Option[String]] ==== Some(s)
        }
        Get(testURI) ~> testRoutes(route) ~> check {
          responseAs[Option[String]] ==== Some(default)
        }
    }

    "queryParam for ? FOO" in Prop.forAll { (n: Int) =>
      val route: Route = (get & queryparam("param".as[FOO].?)) {
        complete(_)
      }
      val uri = testURI.withQuery("param" -> n.toString)
      Get(uri) ~> testRoutes(route) ~> check {
        responseAs[Option[FOO]] ==== Some(FOO(n))
      }
      Get(testURI) ~> testRoutes(route) ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "The requested resource could not be found."
      }
    }

    "queryParam for ? FOO with default" in Prop.forAll { (n: Int, default: Int) =>
      val route: Route = (get & queryparam("param".?[FOO](FOO(default)))) { foo =>
        complete(foo)
      }
      val uri = testURI.withQuery("param" -> n.toString)
      Get(uri) ~> testRoutes(route) ~> check {
        responseAs[Option[FOO]] ==== Some(FOO(n))
      }
      Get(testURI) ~> testRoutes(route) ~> check {
        responseAs[Option[FOO]] ==== Some(FOO(default))
      }
    }

    "queryParam for ? BAR" in Prop.forAll(Gen.alphaStr) { (s: String) =>
      val route: Route = (get & queryparam("param".as[BAR].?)) {
        complete(_)
      }
      val uri = testURI.withQuery("param" -> s)
      Get(uri) ~> testRoutes(route) ~> check {
        responseAs[Option[BAR]] ==== Some(BAR(s))
      }
      Get(testURI) ~> testRoutes(route) ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "The requested resource could not be found."
      }
    }

    "queryParam for ? BAR with default" in Prop.forAll(Gen.alphaStr, Gen.alphaStr) {
      (s: String, default: String) =>
        val route: Route =
          (get & queryparam("param".?[BAR](BAR(default)))) { foo =>
            complete(foo)
          }
        val uri = testURI.withQuery("param" -> s)
        Get(uri) ~> testRoutes(route) ~> check {
          responseAs[Option[BAR]] ==== Some(BAR(s))
        }
        Get(testURI) ~> testRoutes(route) ~> check {
          responseAs[Option[BAR]] ==== Some(BAR(default))
        }
    }

    "queryParam with tuple" in Prop.forAll(
      Gen.choose(Int.MinValue, Int.MaxValue),
      Gen.alphaStr,
      Gen.alphaStr,
      Gen.alphaStr,
      Gen.choose(Int.MinValue, Int.MaxValue)) {
      (foo: Int, bar: String, st: String, stdefault: String, n: Int) =>
        val route: Route =
          (get & queryparam(Tuple4("foo".as[FOO],
            "bar".as[BAR],
            "stest".?[String](stdefault),
            "itest".as[Int]))) { (param, bar, st, n) =>
            val request = FOOBARrequest(param, bar, st, n)
            complete(request)
          }
        val uri = testURI.withQuery("foo" -> foo.toString, "bar" -> bar, "stest" -> st, "itest" -> n.toString)
        Get(uri) ~> testRoutes(route) ~> check {
          val result = responseAs[FOOBARrequest]
          result.bar ==== BAR(bar)
          result.foo ==== FOO(foo)
          result.st ==== st
          result.n ==== n
        }
    }
  }
}
