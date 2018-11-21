package com.ntoggle.kubitschek
package domain

import com.ntoggle.albi.{SupplyPartnerId, DemandPartnerId}
import play.api.libs.json.{Json, Format}

import scalaz.{Show, Order}
import scalaz.std.tuple._

case class RouterConfigurationId(
  dpId: DemandPartnerId,
  spId: SupplyPartnerId)
object RouterConfigurationId {
  implicit val formatPublishConfigurationId: Format[RouterConfigurationId] =
    Json.format[RouterConfigurationId]
  implicit val orderPublishConfigurationId: Order[RouterConfigurationId] =
    Order[(DemandPartnerId, SupplyPartnerId)]
      .contramap(id => (id.dpId, id.spId))
  implicit val showPublishConfigurationId: Show[RouterConfigurationId] =
    Show.showFromToString
}
