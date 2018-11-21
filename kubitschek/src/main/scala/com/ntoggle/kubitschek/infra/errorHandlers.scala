package com.ntoggle.kubitschek.infra

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.fasterxml.jackson.core.JsonParseException
import com.typesafe.scalalogging.slf4j.LazyLogging
import play.api.libs.json._

import scala.util.control.NonFatal

case class ErrorMessage(message: String, cause: Option[JsValue] = None)

object ErrorMessage {
  implicit val errorFormat = Json.format[ErrorMessage]
}


object CustomErrorHandlers extends LazyLogging {
  import PlayJsonSupportExt._

  def flattenJsError(error: JsError): Map[String, Seq[String]] =
    JsError
      .toFlatForm(error)
      .toMap
      .mapValues(_.flatMap(_.messages))

  private def safeParseJson(str: String): JsValue = {
    try {
      Json.parse(str)
    } catch {
      case _: JsonParseException =>
        JsString(str)
    }
  }

  def rejectionHandler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case a: QueryExtractorRejection =>
          complete(BadRequest -> ErrorMessage(a.message, Some(a.cause)))
        case a: PathExtractorRejection =>
          complete(BadRequest -> ErrorMessage(a.message, Some(a.cause)))
        case PlayJsonRejection(e) =>
          complete(BadRequest -> ErrorMessage("JSON payload was not as expected.", Some(Json.toJson(flattenJsError(e)))))
        case NotFoundRejection(msg, cause) =>
          complete(NotFound -> ErrorMessage(msg, cause))
        case BadRequestRejection(msg, cause) =>
          complete(BadRequest -> ErrorMessage(msg, cause))
        case MissingQueryParamRejection(parameterName) =>
          complete(NotFound -> ErrorMessage("missing query parameter.", Some(JsString(parameterName))))
        case MissingFormFieldRejection(parameterName) =>
          complete(NotFound -> ErrorMessage("missing form parameter.", Some(JsString(parameterName))))
        case MalformedRequestContentRejection(msg, _) =>
          complete(BadRequest -> ErrorMessage("The request content was malformed.", Some(safeParseJson(msg))))
        case AuthorizationRejection(msg) =>
          complete(Unauthorized -> ErrorMessage("The user is not authorized to access this resource", Some(safeParseJson(msg))))
        case AuthenticationRejection(msg) =>
          complete(Unauthorized -> ErrorMessage("The user is not authenticated to access this resource", Some(safeParseJson(msg))))
        case ESRejection(msg) =>
          complete(ServiceUnavailable -> ErrorMessage(
            "Elasticsearch cluster/index is unavailable",
            Some(safeParseJson(msg))))
      }
      .handleNotFound {
        complete(NotFound -> ErrorMessage("The requested resource could not be found."))
      }
      .result()

  def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case RejectionError(r) =>
      logger.warn(s"Rejecting request, '$r'")
      rejectionHandler.apply(List(r)).getOrElse {
        logger.error(s"Rejection was unhandled, '$r'")
        complete(InternalServerError -> ErrorMessage("Error during processing of request"))
      }
    case NonFatal(e:org.elasticsearch.client.transport.NoNodeAvailableException) =>
      complete(ServiceUnavailable -> ErrorMessage(
        "Elasticsearch cluster/index is unavailable"))
    case NonFatal(e) =>
      logger.warn("Error during processing of request", e)
      complete(InternalServerError -> ErrorMessage("Error during processing of request"))
  }

}

