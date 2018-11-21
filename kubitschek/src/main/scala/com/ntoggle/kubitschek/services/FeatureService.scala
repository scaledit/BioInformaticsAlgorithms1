package com.ntoggle.kubitschek.services

import com.ntoggle.albi.TrafficType
import com.ntoggle.albi.{SupplyPartnerId, DemandPartnerId}
import com.ntoggle.goldengate.elasticsearch.Size
import com.ntoggle.humber.catalog.CatalogApi.{SuggestRequestString, SuggestOutputText}
import com.ntoggle.humber.catalog.ESCatalog.AutoCompleteError
import com.ntoggle.kubitschek.api.{ApiResponseFuture, FeaturePair, GetFeatureParamRequest, GetFeatureResponse}
import com.ntoggle.kubitschek.catalog.SuggestId
import com.ntoggle.kubitschek.domain._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{EitherT, \/}
import scalaz.std.scalaFuture._

object FeatureService {
  def get(
    getFeatures: (DemandPartnerId,
      SupplyPartnerId,
      TrafficType,
      Size,
      AttributeType,
      SuggestRequestString) => Future[AutoCompleteError \/ List[(SuggestId, SuggestOutputText)]])
    (implicit ctx: ExecutionContext):
  GetFeatureParamRequest => ApiResponseFuture[GetFeatureResponse] = {
    req => EitherT.eitherT(getFeatures(req.dpId, req.spId, req.trafficType,
          req.size, req.attr, req.q).map(_.map {
          n =>
            GetFeatureResponse(
              n.map {
                case (key, value) => FeaturePair(key, value)})}))
          .leftMap(e =>
          ConfigurationErrorRejections.rejection(GeneralError(e.msg)))
      }
  }
