package com.ntoggle.kubitschek
package api

import com.ntoggle.albi._
import com.ntoggle.albi.TrafficType
import com.ntoggle.albi.users.UserListId
import com.ntoggle.goldengate.elasticsearch.Size
import com.ntoggle.goldengate.playjson.PlayFormat._
import com.ntoggle.goldengate.scalazint.EqualzAndShowz
import com.ntoggle.humber.catalog.CatalogApi.{SuggestOutputText, SuggestRequestString}
import com.ntoggle.kubitschek.catalog.SuggestId
import com.ntoggle.kubitschek.domain._
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scalaz.Order
import scalaz.std.anyVal.intInstance

case class CreateDemandPartnerRequest(name: DemandPartnerName)
object CreateDemandPartnerRequest
  extends EqualzAndShowz[CreateDemandPartnerRequest] {
  implicit val CreateDemandPartnerFormat: Format[CreateDemandPartnerRequest] = Json.format[CreateDemandPartnerRequest]
}

case class CreateSupplyPartnerRequest(
  name: SupplyPartnerName)
object CreateSupplyPartnerRequest extends EqualzAndShowz[CreateSupplyPartnerRequest] {
  implicit val CreateSupplyPartnerFormat: Format[CreateSupplyPartnerRequest] =
    Json.format[CreateSupplyPartnerRequest]
}

case class Port(value: Int)
object Port {
  val InvalidPortError = ValidationError("Invalid Port")
  val maxPort = 65536
  val minPort = 0
  val readPort: Reads[Port] =
    Reads.filter[Int](InvalidPortError)(p => p > minPort && p < maxPort).map(Port.apply)
  val writePort: Writes[Port] =
    implicitly[Writes[Int]].contramap(p => p.value)
  implicit val formatPort: Format[Port] = Format(readPort, writePort)
  implicit val orderPort: Order[Port] = Order[Int].contramap(_.value)
}

case class RouterConfigurationRequest(
  configEndpoint: ConfigurationEndpoint,
  maxQps: DemandPartnerMaxQps)
object RouterConfigurationRequest extends EqualzAndShowz[RouterConfigurationRequest] {
  implicit val RouterConfigurationRequestFormat: Format[RouterConfigurationRequest] =
    Json.format[RouterConfigurationRequest]
}

case class RouterConfigurationUpdateRequest(
  configEndpoint: ConfigurationEndpoint)

object RouterConfigurationUpdateRequest extends EqualzAndShowz[RouterConfigurationUpdateRequest] {
  implicit val RouterConfigurationUpdateRequestFormat: Format[RouterConfigurationUpdateRequest] =
    Json.format[RouterConfigurationUpdateRequest]
}

/**
  * This is returned when querying for a router configuration, which may be associated with many Version objects.
  */
case class RouterConfigurationResponse(
  dpId: DemandPartnerId,
  spId: SupplyPartnerId,
  configEndpoint: ConfigurationEndpoint)
object RouterConfigurationResponse extends EqualzAndShowz[RouterConfigurationResponse] {
  implicit val RouterConfigurationResponseFormat: Format[RouterConfigurationResponse] =
    Json.format[RouterConfigurationResponse]
}

/**
  * This is returned when creating a new configuration because a new Version object is also created.
  */
case class CreateRouterConfigurationResponse(
  vId: VersionId,
  dpId: DemandPartnerId,
  spId: SupplyPartnerId,
  maxQps: DemandPartnerMaxQps,
  configEndpoint: ConfigurationEndpoint)
object CreateRouterConfigurationResponse extends EqualzAndShowz[CreateRouterConfigurationResponse] {
  implicit val CreateRouterConfigurationResponseFormat: Format[CreateRouterConfigurationResponse] =
    Json.format[CreateRouterConfigurationResponse]
}

case class RuleConditionPair[A](key: A, value: String)
object RuleConditionPair {
  implicit def writes[A: Writes]: Writes[RuleConditionPair[A]] = (
    (__ \ "key").write[A] ~
      (__ \ "value").write[String]
    ) (rcp => (rcp.key, rcp.value))

  implicit def read[A: Reads]: Reads[RuleConditionPair[A]] = (
    (__ \ "key").read[A] ~
      (__ \ "value").read[String]
    ) (RuleConditionPair.apply[A] _)
}

case class RuleConditionResponse[A](
  default: DefaultConditionAction,
  exceptions: Set[RuleConditionPair[A]],
  undefined: UndefinedConditionAction)
object RuleConditionResponse {
  def writes[A: Writes]: Writes[RuleConditionResponse[A]] = (
    (__ \ "default").write[DefaultConditionAction] ~
      (__ \ "exceptions").write[List[RuleConditionPair[A]]].contramap[Set[RuleConditionPair[A]]](_.toList) ~
      (__ \ "undefined").write[UndefinedConditionAction]
    ) (cond => (cond.default, cond.exceptions, cond.undefined))

  def reads[A: Reads]: Reads[RuleConditionResponse[A]] = (
    (__ \ "default").read[DefaultConditionAction] ~
      (__ \ "exceptions").read[List[RuleConditionPair[A]]].map(_.toSet) ~
      (__ \ "undefined").read[UndefinedConditionAction]
    ) (RuleConditionResponse.apply[A] _)

  implicit def formats[A: Format]: Format[RuleConditionResponse[A]] =
    Format[RuleConditionResponse[A]](reads[A], writes[A])
}

case class RuleConditionsResponse(
  carrier: Option[RuleConditionResponse[CarrierId]],
  handset: Option[RuleConditionResponse[HandsetId]],
  deviceIdExistence: Option[RuleConditionResponse[DeviceIdType]],
  os: Option[RuleConditionResponse[OsId]],
  wifi: Option[RuleConditionResponse[ConnectionType]],
  ipAddressExistence: Option[RuleConditionResponse[IpAddressType]],
  appAndSite: Option[RuleConditionResponse[AppId]],
  banner: Option[RuleConditionResponse[BannerType]],
  mraid: Option[RuleConditionResponse[MraidType]],
  interstitial: Option[RuleConditionResponse[InterstitialType]],
  video: Option[RuleConditionResponse[VideoType]],
  native: Option[RuleConditionResponse[NativeType]],
  adSize: Option[RuleConditionResponse[AdSizeId]],
  latlong: Option[RuleConditionResponse[LatLongType]],
  country: Option[RuleConditionResponse[CountryIdAlpha2]],
  region: Option[RuleConditionResponse[RegionId]],
  city: Option[RuleConditionResponse[CityName]],
  zip: Option[RuleConditionResponse[ZipAlpha]],
  userList: Option[RuleConditionResponse[UserListName]])
object RuleConditionsResponse extends EqualzAndShowz[RuleConditionsResponse] {
  implicit val RuleConditionsResponseFormat: Format[RuleConditionsResponse] =
    Json.format[RuleConditionsResponse]
}

case class GetRuleResponse(
  id: RuleId,
  name: RuleName,
  created: RuleCreatedInstant,
  trafficType: TrafficType,
  conditions: RuleConditionsResponse)
object GetRuleResponse extends EqualzAndShowz[GetRuleResponse] {
  implicit val GetRuleResponseFormat: Format[GetRuleResponse] =
    Json.format[GetRuleResponse]
}

case class CreateRuleRequest(
  name: RuleName,
  trafficType: TrafficType,
  conditions: RuleConditions)
object CreateRuleRequest extends EqualzAndShowz[CreateRuleRequest] {
  implicit val format = Json.format[CreateRuleRequest]
}

case class ReplaceRuleRequest(
  name: RuleName,
  trafficType: TrafficType,
  conditions: RuleConditions)
object ReplaceRuleRequest extends EqualzAndShowz[ReplaceRuleRequest] {
  implicit val format = Json.format[ReplaceRuleRequest]
}

case class GetFeatureParamRequest(
  dpId: DemandPartnerId,
  spId: SupplyPartnerId,
  trafficType: TrafficType,
  size: Size,
  attr: AttributeType,
  q: SuggestRequestString)

case class FeaturePair(key: SuggestId, value: SuggestOutputText)
object FeaturePair {
  implicit val SuggestOutputTextFormat: Format[SuggestOutputText]
  = implicitly[Format[String]].inmap[SuggestOutputText](SuggestOutputText.apply, _.value)
  implicit val FeaturePairFormat: Format[FeaturePair] = (
    (__ \ "key").format[SuggestId] ~
      (__ \ "value").format[SuggestOutputText]
    ) (FeaturePair.apply, unlift(FeaturePair.unapply))
}

case class GetFeatureResponse(
  result: List[FeaturePair])
object GetFeatureResponse {
  implicit val GetFeatureResponseFormat: Format[GetFeatureResponse] =
    Json.format[GetFeatureResponse]
}

case class VersionSummaryResponse(
  id: VersionId,
  dpId: DemandPartnerId,
  spId: SupplyPartnerId,
  created: CreatedInstant,
  modified: ModifiedInstant,
  published: Option[PublishedInstant],
  maxQps: DemandPartnerMaxQps,
  availableQps: EndpointAvailableForecast,
  requests: MetricCount,
  bids: MetricCount,
  rules: List[RuleSummary])

object VersionSummaryResponse {
  implicit val VersionSummaryResponseFormat: Format[VersionSummaryResponse] =
    Json.format[VersionSummaryResponse]
}

case class UserVersionCreateRequest(
  spId: SupplyPartnerId,
  maxQps: DemandPartnerMaxQps
)

object UserVersionCreateRequest {
  implicit val UserVersionCreateRequestFormat: Format[UserVersionCreateRequest] =
    Json.format[UserVersionCreateRequest]
}

case class VersionCreateRequest(
  dpId: DemandPartnerId,
  spId: SupplyPartnerId,
  maxQps: DemandPartnerMaxQps
)

object VersionCreateRequest {
  implicit val VersionCreateRequestFormat: Format[VersionCreateRequest] =
    Json.format[VersionCreateRequest]
}

case class VersionQpsUpdateRequest(
  maxQps: DemandPartnerMaxQps,
  rules: List[RulesQpsUpdate]
)
object VersionQpsUpdateRequest {
  implicit val VersionQpsUpdateRequest: Format[VersionQpsUpdateRequest] =
    Json.format[VersionQpsUpdateRequest]
}

case class RulesQpsUpdate(
  id: RuleId,
  desiredQps: DesiredToggledQps
)
object RulesQpsUpdate {
  implicit val RuleQpsUpdate: Format[RulesQpsUpdate] =
    Json.format[RulesQpsUpdate]
}

case class VersionQpsUpdateResponse(
  id: VersionId,
  maxQps: DemandPartnerMaxQps,
  rules: List[RulesQpsUpdate]
)
object VersionQpsUpdateResponse {
  implicit val VersionQpsUpdateResponse: Format[VersionQpsUpdateResponse] =
    Json.format[VersionQpsUpdateResponse]

}

case class RuleSummary(
  id: RuleId,
  trafficType: TrafficType,
  desiredQps: DesiredToggledQps,
  actuals: MetricCount,
  available: RuleAvailableForecast,
  created: RuleCreatedInstant,
  name: RuleName)
object RuleSummary extends EqualzAndShowz[RuleSummary] {
  implicit val formatRuleSummary: Format[RuleSummary] =
    Json.format[RuleSummary]
}

case class GetForecastResponse(
  estimatedQPS: EstimatedAvailableQps)
object GetForecastResponse {
  implicit val formatGetForecastResponse: Format[GetForecastResponse] =
    Json.format[GetForecastResponse]
}

case class ParsedItem[A](key: A, value: String)
object ParsedItem {
  implicit def ParsedItemFormat[A: Format]: Format[ParsedItem[A]] = (
    (__ \ "key").format[A] ~
      (__ \ "value").format[String]
    ) (apply[A], unapply[A](_).get)
}

case class FailedItem(item: String, reason: String)
object FailedItem
  extends EqualzAndShowz[FailedItem] {
  implicit val FailedItemFormat: Format[FailedItem] = Json.format[FailedItem]
}

case class ValidateCityRequest(data: List[String])
object ValidateCityRequest
  extends EqualzAndShowz[ValidateCityRequest] {
  implicit val ValidateCityRequestFormat: Format[ValidateCityRequest] = Json.format[ValidateCityRequest]
}

case class ValidateCityResponse(failedItems: List[FailedItem], parsedItems: List[ParsedItem[CityName]])
object ValidateCityResponse
  extends EqualzAndShowz[ValidateCityResponse] {
  implicit val ValidateCityResponseFormat: Format[ValidateCityResponse] = Json.format[ValidateCityResponse]
}

case class ValidateCountryRequest(data: List[String])
object ValidateCountryRequest
  extends EqualzAndShowz[ValidateCountryRequest] {
  implicit val ValidateCountryRequestFormat: Format[ValidateCountryRequest] = Json.format[ValidateCountryRequest]
}

case class ValidateCountryResponse(failedItems: List[FailedItem], parsedItems: List[ParsedItem[CountryIdAlpha2]])
object ValidateCountryResponse
  extends EqualzAndShowz[ValidateCountryResponse] {
  implicit val ValidateCountryResponseFormat: Format[ValidateCountryResponse] = Json.format[ValidateCountryResponse]
}

case class ValidateZipRequest(data: List[String])
object ValidateZipRequest
  extends EqualzAndShowz[ValidateZipRequest] {
  implicit val ValidateCountryRequestFormat: Format[ValidateZipRequest] = Json.format[ValidateZipRequest]
}

case class ValidateZipResponse(failedItems: List[FailedItem], parsedItems: List[ParsedItem[ZipAlpha]])
object ValidateZipResponse
  extends EqualzAndShowz[ValidateZipResponse] {
  implicit val ValidateZipResponseFormat: Format[ValidateZipResponse] = Json.format[ValidateZipResponse]
}

case class ValidateRegionRequest(data: List[String])
object ValidateRegionRequest
  extends EqualzAndShowz[ValidateRegionRequest] {
  implicit val ValidateRegionRequestFormat: Format[ValidateRegionRequest] = Json.format[ValidateRegionRequest]
}

case class ValidateRegionResponse(failedItems: List[FailedItem], parsedItems: List[ParsedItem[RegionId]])
object ValidateRegionResponse
  extends EqualzAndShowz[ValidateRegionResponse] {
  implicit val ValidateRegionResponseFormat: Format[ValidateRegionResponse] = Json.format[ValidateRegionResponse]
}

case class ValidateCarrierRequest(data: List[String])
object ValidateCarrierRequest
  extends EqualzAndShowz[ValidateCarrierRequest] {
  implicit val ValidateCarrierRequestFormat: Format[ValidateCarrierRequest] = Json.format[ValidateCarrierRequest]
}

case class ValidateCarrierResponse(failedItems: List[FailedItem], parsedItems: List[ParsedItem[CarrierId]])
object ValidateCarrierResponse
  extends EqualzAndShowz[ValidateCarrierResponse] {
  implicit val ValidateCarrierResponseFormat: Format[ValidateCarrierResponse] = Json.format[ValidateCarrierResponse]
}

case class ValidateOsRequest(data: List[String])
object ValidateOsRequest
  extends EqualzAndShowz[ValidateOsRequest] {
  implicit val ValidateOsRequestFormat: Format[ValidateOsRequest] = Json.format[ValidateOsRequest]
}

case class ValidateOsResponse(failedItems: List[FailedItem], parsedItems: List[ParsedItem[OsId]])
object ValidateOsResponse
  extends EqualzAndShowz[ValidateOsResponse] {
  implicit val ValidateOsResponseFormat: Format[ValidateOsResponse] = Json.format[ValidateOsResponse]
}

case class ValidateHandsetRequest(data: List[String])
object ValidateHandsetRequest
  extends EqualzAndShowz[ValidateHandsetRequest] {
  implicit val ValidateHandsetRequestFormat: Format[ValidateHandsetRequest] = Json.format[ValidateHandsetRequest]
}

case class ValidateHandsetResponse(failedItems: List[FailedItem], parsedItems: List[ParsedItem[HandsetId]])
object ValidateHandsetResponse
  extends EqualzAndShowz[ValidateHandsetResponse] {
  implicit val ValidateHandsetResponseFormat: Format[ValidateHandsetResponse] = Json.format[ValidateHandsetResponse]
}

case class ValidateAppRequest(data: List[String])
object ValidateAppRequest
  extends EqualzAndShowz[ValidateHandsetRequest] {
  implicit val ValidateAppRequestFormat: Format[ValidateAppRequest] = Json.format[ValidateAppRequest]
}

case class ValidateAppResponse(failedItems: List[FailedItem], parsedItems: List[ParsedItem[AppId]])
object ValidateAppResponse
  extends EqualzAndShowz[ValidateAppResponse] {
  implicit val ValidateAppResponseFormat: Format[ValidateAppResponse] = Json.format[ValidateAppResponse]
}
