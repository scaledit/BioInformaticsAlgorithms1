package com.ntoggle.kubitschek.utils

import java.util.UUID

import com.ntoggle.albi.{SupplyPartnerId, DemandPartnerId, SupplyPartnerName, SupplyPartner}
import com.ntoggle.kubitschek.domain.RouterConfigurationId
import com.ntoggle.kubitschek.integration._

object UserUtils {

  val newId = () => UUID.randomUUID().toString

  def getUser(rcId:RouterConfigurationId) = {
    AuthenticatedUser(
      Username("bob"),
      EmailAddress("12345@test.com"),
      Fullname("bob here"),
      UserPreferences.Default,
      Organization(id=DemandPartnerOrganizationId(rcId.dpId),name="demand"),
      DemandConfiguration(List(SupplyPartner(rcId.spId,SupplyPartnerName(""))))
    )
  }

  def getUserBadDemandPartner(rcId:RouterConfigurationId) = {
    AuthenticatedUser(
      Username("bob"),
      EmailAddress("12345@test.com"),
      Fullname("bob here"),
      UserPreferences.Default,
      Organization(id=DemandPartnerOrganizationId(DemandPartnerId(newId())),name="demand"),
      DemandConfiguration(List(SupplyPartner(rcId.spId,SupplyPartnerName(""))))
    )
  }

 def getUserBadSupplyPartner(rcId:RouterConfigurationId) = {
    AuthenticatedUser(
      Username("bob"),
      EmailAddress("12345@test.com"),
      Fullname("bob here"),
      UserPreferences.Default,
      Organization(id=DemandPartnerOrganizationId(rcId.dpId),name="demand"),
      DemandConfiguration(List(SupplyPartner(SupplyPartnerId(newId()),SupplyPartnerName(""))))
    )
  }

}
