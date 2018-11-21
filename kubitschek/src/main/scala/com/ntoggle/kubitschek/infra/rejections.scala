package com.ntoggle.kubitschek
package infra

import akka.http.scaladsl.server.Rejection
import play.api.libs.json.{JsString, JsObject, JsValue, JsError}

case class PathExtractorRejection(error:ParamExtractorError)
  extends Rejection {
  val message: String = "Path component was not as expected"
  def cause: JsValue = ParamExtractorRejection.cause(error)
}
case class QueryExtractorRejection(error:ParamExtractorError)
  extends Rejection {
  val message: String = "Query component was not as expected"
  def cause: JsValue = ParamExtractorRejection.cause(error)
}
object ParamExtractorRejection {
  object Keys {
    val ExpectedType = "expectedType"
    val Actual = "actual"
  }
  def cause(r: ParamExtractorError): JsValue =
    JsObject(Map(
      Keys.ExpectedType -> JsString(r.name),
      Keys.Actual -> JsString(r.actual)))
}
case class BadRequestRejection(message: String, cause: Option[JsValue]) extends Rejection
case class NotFoundRejection(message: String, cause: Option[JsValue]) extends Rejection
case class PlayJsonRejection(error: JsError) extends Rejection
case class AuthenticationRejection(message: String) extends Rejection
case class AuthorizationRejection(message: String) extends Rejection
case class ESRejection(message:String) extends Rejection
