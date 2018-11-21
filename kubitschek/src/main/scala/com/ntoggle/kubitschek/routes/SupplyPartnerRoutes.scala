package com.ntoggle.kubitschek
package routes

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Rejection, RejectionError, Route}
import akka.stream.Materializer
import com.ntoggle.albi.TrafficType
import com.ntoggle.albi._
import com.ntoggle.goldengate.elasticsearch.{IndexName, Size}
import com.ntoggle.humber.catalog.CatalogApi.SuggestRequestString
import com.ntoggle.humber.catalog.FeatureSpecificIndex
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.application.FeatureIndexConfig
import com.ntoggle.kubitschek.domain.{RuleConditions, AttributeType, Limit, Offset}
import com.ntoggle.kubitschek.infra._
import com.ntoggle.kubitschek.integration.{AuthenticatedUser, UnauthorizedAccess}
import com.ntoggle.kubitschek.services.ValidateService

import scala.concurrent.Future
import scala.util.{Success, Failure}
import scalaz.\/
import scalaz.syntax.equal._
import scalaz.std.anyVal.intInstance


object SupplyPartnerRoutes
  extends ValidatedDirectives
  with ParamExtractorDirectives
  with SecurityDirectives {

  import ApiParamExtractors._
  import PlayJsonSupportExt._
  def route(
    //featureConfig: FeatureIndexConfig,
    createSupplyPartner: (SupplyPartnerName) =>
      ApiResponseFuture[SupplyPartner],
    getSupplyPartner: SupplyPartnerId => Future[Option[SupplyPartner]],
    listSupplyPartners: (Offset, Limit) => Future[List[SupplyPartner]],
    getFeatures: (GetFeatureParamRequest) => ApiResponseFuture[GetFeatureResponse],
    getForecast: (SupplyPartnerId, DemandPartnerId, TrafficType, RuleConditions) => ApiResponseFuture[GetForecastResponse],
    checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser]
  )(implicit materializer: Materializer): Route =
    pathPrefix("supply-partners") {
      pathPrefix(pathValue[SupplyPartnerId] / "features") { spId => {
        authenticate(checkAuthentication) { user =>
          authorizeSp(spId, user) { (userDpId, userSpId) =>
            (get & queryparam(Tuple4(
              "trafficType".as[TrafficType],
              "size".as[Int],
              "attr".as[AttributeType],
              "q".as[SuggestRequestString])) & pathEnd) {
              (trafficType, size, attr, q) =>
                val paramreq = GetFeatureParamRequest(userDpId, userSpId, trafficType,
                  Size(size), attr, q)
                onApiResponseFutureComplete(
                  getFeatures(paramreq)) {
                  case Failure(e) => reject(ESRejection(
                    s"ES index not found for ")) //'${
                  //  featureConfig.fromAttributeType(attr)
                  //    .forSupplyPartnerAndTraffic(spId, trafficType)}'"))
                  case s: Success[GetFeatureResponse] => complete(s)
                }
            } ~
              (post & path("validate" / "os") & validateJson[ValidateOsRequest] & pathEnd) {
                (r) => complete(ValidateService.validateOs(r))
              } ~
              (post & path("validate" / "country") & validateJson[ValidateCountryRequest]) { r =>
                complete(ValidateService.validateCountry(r))
              } ~
              (post & path("validate" / "city") & validateJson[ValidateCityRequest]) { r =>
                val parsed = for {
                  a <- r.data
                } yield ParsedItem(CityName(a), a)
                complete(ValidateCityResponse(List.empty, parsed))
              } ~
              (post & path("validate" / "zip") & validateJson[ValidateZipRequest]) { r =>
                val parsed = for {
                  a <- r.data
                } yield ParsedItem(ZipAlpha(a), a)
                complete(ValidateZipResponse(List.empty, parsed))
              } ~
              (post & path("validate" / "carrier") & validateJson[ValidateCarrierRequest]) { r =>
                val parsed = for {
                  a <- r.data
                } yield ParsedItem(CarrierId(a), a)
                complete(ValidateCarrierResponse(List.empty, parsed))
              } ~
              (post & path("validate" / "region") & validateJson[ValidateRegionRequest]) { r =>
                complete(ValidateService.validateRegion(r))
              } ~
              (post & path("validate" / "handset") & validateJson[ValidateHandsetRequest]) { r =>
                complete(ValidateService.validateHandset(r))
              } ~
              (post & path("validate" / "appAndSite") & validateJson[ValidateAppRequest]) { r =>
                val parsed = for {
                  a <- r.data
                } yield ParsedItem(AppId(a), a)
                complete(ValidateAppResponse(List.empty, parsed))
              }
          }
        }
      }
      } ~
        (post & path(pathValue[SupplyPartnerId] / "traffic-type"/ pathValue[TrafficType] / "forecast") & validateJson[RuleConditions]) {
          (spId, trafficType, ruleConditions) =>
            authenticate(checkAuthentication) { user => {
              authorizeSp(spId, user) { (userDpId, userSpId) =>
                onApiResponseFutureComplete(
                  getForecast(
                    userSpId,
                    userDpId,
                    trafficType,
                    ruleConditions))(complete(_))
              }
            }
            }
        } ~
        //        authorize(checkAuthorization) { auth =>
        (get & pathEnd & queryparam("limit".as[Int] -> "offset".as[Int])) {
          (limit, offset) =>
            onComplete(listSupplyPartners(Offset(offset), Limit(limit))) { sp =>
              complete(sp)
            }
        } ~
        (get & path(pathValue[SupplyPartnerId])) { spId =>
          onComplete(getSupplyPartner(spId)) { sp =>
            complete(sp)
          }
        } ~
        (post & pathEnd & validateJson[CreateSupplyPartnerRequest]) { dpReq =>
          onApiResponseFutureComplete(
            createSupplyPartner(
              dpReq.name))(complete(_))
        }
    }
}