package com.ntoggle.kubitschek
package domain

import com.ntoggle.goldengate.scalazint.EqualzAndShowz
import play.api.libs.json._

case class Version(
  id: VersionId,
  routerConfigurationId: RouterConfigurationId,
  created: CreatedInstant,
  modified: ModifiedInstant,
  published: Option[PublishedInstant],
  maxQps: DemandPartnerMaxQps)

object Version
  extends EqualzAndShowz[Version] {
  implicit val formatVersion: Format[Version] =
    Json.format[Version]
}
