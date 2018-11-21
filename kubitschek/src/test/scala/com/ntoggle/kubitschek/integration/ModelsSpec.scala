package com.ntoggle.kubitschek.integration

import com.ntoggle.albi.{SupplyPartnerId, DemandPartnerId}
import com.ntoggle.goldengate.playjson.test.JsonTests
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers

import scalaz.scalacheck.ScalazProperties

class ModelsSpec
  extends Specification
  with ScalaCheck
  with JsonTests
  with ScalazMatchers {

  import IntegrationGenerators._

  "OrganizationId.equal" ! ScalazProperties.equal.laws[OrganizationId]
  "OrganizationId.format" ! checkFormat[OrganizationId]
  "OrganizationId DemandPartnerOrganizationId expected writes" !
    expectedWritesTest(
      OrganizationId.dpId(
        DemandPartnerId("12345")),
      """{ "dpId": "12345" }""")
  "OrganizationId SupplyPartnerOrganizationId expected writes" !
    expectedWritesTest(
      OrganizationId.spId(
        SupplyPartnerId("12345")),
      """{ "spId": "12345" }""")


}
