package com.ntoggle.kubitschek
package integration

import com.ntoggle.albi.AlbiGenerators
import org.scalacheck.{Arbitrary, Gen}

object IntegrationGenerators {
  import AlbiGenerators._

  def genOrganizationId: Gen[OrganizationId] =
    Gen.oneOf(
      genDemandPartnerId.map(OrganizationId.dpId),
      genSupplyPartnerId.map(OrganizationId.spId))
  implicit def arbOrganizationId: Arbitrary[OrganizationId] =
    Arbitrary(genOrganizationId)
}
