package com.ntoggle.kubitschek.catalog

import com.ntoggle.albi._
import com.ntoggle.albi.TrafficType
import com.ntoggle.goldengate.elasticsearch._
import com.ntoggle.humber.catalog.CatalogApi.{SuggestOutputText, SuggestRequestString}
import com.ntoggle.humber.catalog.ESCatalog._
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.services.ConfigurationErrorRejections
import play.api.libs.functional.syntax._
import play.api.libs.json.Format

import scala.concurrent.{ExecutionContext, Future}
import scalaz._

case class SuggestId(value: String) extends AnyVal
object SuggestId {
  def from[A: SuggestIdable](a: A): SuggestId =
    SuggestId(SuggestIdable[A].toPayload(a).value)
  implicit val SuggestIdFormat: Format[SuggestId] =
    implicitly[Format[String]].inmap[SuggestId](SuggestId.apply, _.value)
}
trait SuggestIdable[A] {
  def toPayload(a: A): SuggestId
}
object SuggestIdable {
  def apply[A: SuggestIdable]: SuggestIdable[A] = implicitly[SuggestIdable[A]]
  def from[A](f: A => String): SuggestIdable[A] = new SuggestIdable[A] {
    def toPayload(a: A): SuggestId = SuggestId(f(a))
  }
}
object AttributeSuggestIdableImplicits {
  implicit val AppSuggestIdable: SuggestIdable[AppId] =
    SuggestIdable.from(_.value)
  implicit val HandsetSuggestIdable: SuggestIdable[HandsetId] =
    SuggestIdable.from(HandsetId.toStringKey)
  implicit val CarrierIdSuggestIdable: SuggestIdable[CarrierId] =
    SuggestIdable.from(_.value)
  implicit val OsSuggestIdable: SuggestIdable[OsId] =
    SuggestIdable.from(OsId.toStringKey)
  implicit val CitySuggestIdable: SuggestIdable[City] =
    SuggestIdable.from(_.name.value)
  implicit val ZipSuggestIdable: SuggestIdable[ZipAlpha] =
    SuggestIdable.from(_.value)
  implicit val CountrySuggestIdable: SuggestIdable[Country] =
    SuggestIdable.from(_.idAlpha2.value.toString)
  implicit val RegionSuggestIdable: SuggestIdable[Region] =
    SuggestIdable.from(r => RegionId.toStringKey(r.id))
}
class FeatureIndexClient(
  getUserLists: (Option[DemandPartnerId], Offset, Limit) =>
    Future[List[UserList]],
  appAC: (SuggestRequestString, Size, SupplyPartnerId, TrafficType) =>
    Future[AutoCompleteError \/ List[(AppId, SuggestOutputText)]],
  handsetAC: (SuggestRequestString, Size, SupplyPartnerId, TrafficType) =>
    Future[AutoCompleteError \/ List[(HandsetId, SuggestOutputText)]],
  osAC: (SuggestRequestString, Size, SupplyPartnerId, TrafficType) =>
    Future[AutoCompleteError \/ List[(OsId, SuggestOutputText)]],
  cityAC: (SuggestRequestString, Size, SupplyPartnerId, TrafficType) =>
    Future[AutoCompleteError \/ List[(City, SuggestOutputText)]],
  carrierAC: (SuggestRequestString, Size, SupplyPartnerId, TrafficType) =>
    Future[AutoCompleteError \/ List[(CarrierId, SuggestOutputText)]]) {
  import AttributeSuggestIdableImplicits._
  def autoComplete(
    dpId: DemandPartnerId,
    spId: SupplyPartnerId,
    trafficType: TrafficType,
    size: Size,
    attr: AttributeType,
    q: SuggestRequestString)(implicit ctx: ExecutionContext):
  Future[AutoCompleteError \/ List[(SuggestId, SuggestOutputText)]] = {
    attr match {
      case AppAttr => appAC(q, size, spId, trafficType)
        .map(_.map(_.map(o => (SuggestId.from(o._1), o._2))))
      case HandsetAttr => handsetAC(q, size, spId, trafficType)
        .map(_.map(_.map(o => (SuggestId.from(o._1), o._2))))
      case CountryAttr => Future.successful(\/-(
        Country.all.filter(_.longName.value.toLowerCase
          .split("\\s+").exists(_.startsWith(q.value.toLowerCase)))
          .take(size.value).map(
            c => (SuggestId.from(c), SuggestOutputText(c.shortName.value)))))
      case RegionAttr => Future.successful(\/-(
        Region.all.filter(_.name.value.toLowerCase
          .split("\\s+").exists(_.startsWith(q.value.toLowerCase)))
          .take(size.value).map(
            r => (SuggestId.from(r), SuggestOutputText(r.name.value)))))
      case CityAttr => cityAC(q, size, spId, trafficType)
        .map(_.map(_.map(o => (SuggestId.from(o._1), o._2))))
      case OsAttr => osAC(q, size, spId, trafficType)
        .map(_.map(_.map(o => (SuggestId.from(o._1), o._2))))
      case CarrierAttr => carrierAC(q, size, spId, trafficType)
        .map(_.map(_.map(o => (SuggestId.from(o._1), o._2))))

      case UserListAttr =>

        getUserLists(Some(dpId), Offset.Zero, Limit(size.value)).map( ull => {
            \/-(ull.map {
              ul =>
                (SuggestId(ul.name.value), SuggestOutputText(ul.name.value))
            })
        })

      case _ => Future.successful(
        -\/(AutoCompleteError("Incorrect AttributeType")))
    }
  }
}
