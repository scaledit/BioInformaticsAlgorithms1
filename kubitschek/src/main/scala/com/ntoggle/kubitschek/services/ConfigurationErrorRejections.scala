package com.ntoggle.kubitschek
package services

import akka.http.scaladsl.server.Rejection
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.infra.{AuthorizationRejection, NotFoundRejection, BadRequestRejection}
import play.api.libs.json.Json

object ConfigurationErrorRejections {

  def rejection(e: ConfigurationError): Rejection =
    e match {
      case VersionAlreadyPublished(v) =>
        BadRequestRejection("version already published", Some(Json.toJson(v)))
      case VersionNotFound(v) =>
        NotFoundRejection("version not found", Some(Json.toJson(v)))
      case SupplyPartnerNotFound(s) =>
        BadRequestRejection("supply partner does not exist", Some(Json.toJson(s)))
      case SupplyPartnerNameAlreadyExists(s) =>
        BadRequestRejection("supply partner name already exists", Some(Json.toJson(s)))
      case DemandPartnerNotFound(d) =>
        BadRequestRejection("demand partner does not exist", Some(Json.toJson(d)))
      case DemandPartnerNameAlreadyExists(d) =>
        BadRequestRejection("demand partner name already exists", Some(Json.toJson(d)))
      case RouterConfigurationNotFound(p) =>
        BadRequestRejection("router configuration does not exist", Some(Json.toJson(p)))
      case RuleNotFound(r) =>
        NotFoundRejection("rule not found", Some(Json.toJson(r)))
      case RulesNotFound(l) =>
        BadRequestRejection("rules not found", Some(Json.toJson(l)))
      case RuleNotInVersion(id) =>
        BadRequestRejection("rule not found in version", Some(Json.toJson(id)))
      case RulesNotInVersion(l) =>
        BadRequestRejection("rules not found in version", Some(Json.toJson(l)))
      case UserListIdNotFound(id) =>
        NotFoundRejection("user list not found", Some(Json.toJson(id)))
      case UserListNameNotFound(name) =>
        NotFoundRejection("user list not found by name", Some(Json.toJson(name)))
      case ResourceNotAuthorized() =>
        AuthorizationRejection("not authorized to access resource")
      case MaximumRulesReached(m) =>
        BadRequestRejection("maximum amount of rules reached", Some(Json.toJson(m)))
      case RouterConfigurationAlreadyExists(id) =>
        BadRequestRejection(
          "demand partner/supply partner/supply partner type already exists",
          Some(Json.toJson(id)))
      case GeneralError(m) =>
        BadRequestRejection("error attempting to perform action", Some(Json.toJson(m)))


      // These are somewhat questionable rejections
      case VersionAlreadyExists(v) =>
        BadRequestRejection("version already exists", Some(Json.toJson(v)))
      case SupplyPartnerAlreadyExists(s) =>
        BadRequestRejection("supply partner already exists", Some(Json.toJson(s)))
      case DemandPartnerAlreadyExists(d) =>
        BadRequestRejection("demand partner already exists", Some(Json.toJson(d)))
      case RuleAlreadyExists(r) =>
        BadRequestRejection("rule already exists", Some(Json.toJson(r)))
    }

}
