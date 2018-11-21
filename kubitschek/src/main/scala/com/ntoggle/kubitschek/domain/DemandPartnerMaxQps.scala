package com.ntoggle.kubitschek
package domain

import com.ntoggle.albi.MaxQps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** maximum QPS that supply partner is able to send */
case class DemandPartnerMaxQps(value: MaxQps)
object DemandPartnerMaxQps {
  implicit val format: Format[DemandPartnerMaxQps] =
    implicitly[Format[MaxQps]].inmap(DemandPartnerMaxQps.apply, _.value)
}
