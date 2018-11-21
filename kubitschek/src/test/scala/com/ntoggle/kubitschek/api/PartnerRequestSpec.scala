package com.ntoggle.kubitschek
package api

import com.ntoggle.albi.{DemandPartnerName, SupplyPartnerName}
import com.ntoggle.albi.AlbiGenerators.{genDemandPartnerName, genSupplyPartnerName}
import com.ntoggle.goldengate.NGen
import com.ntoggle.goldengate.playjson.test.JsonTests
import com.ntoggle.kubitschek.Tools.JsonTestsAddition
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers
import play.api.data.validation.ValidationError
import play.api.libs.json._

import scalaz.syntax.apply._
import scalaz.scalacheck.ScalaCheckBinding._
import scalaz.scalacheck.ScalazProperties


class PartnerRequestSpec
  extends Specification
  with JsonTests
  with ScalaCheck
  with ScalazMatchers with JsonTestsAddition {

  "CreateDemandPartnerRequests" should {
    def genCreateDemandPartnerRequest: Gen[CreateDemandPartnerRequest] =
      genDemandPartnerName.map(CreateDemandPartnerRequest.apply)
    implicit def arbCreateDemandPartnerRequests: Arbitrary[CreateDemandPartnerRequest] =
      Arbitrary(genCreateDemandPartnerRequest)

    "CreateDemandPartnerRequest.Equal" ! ScalazProperties.equal.laws[CreateDemandPartnerRequest]

    "Format[CreateDemandPartnerRequest]" ! checkFormat[CreateDemandPartnerRequest]

    "Parse valid input correctly" ! expectedFormatTest(
      CreateDemandPartnerRequest(DemandPartnerName("dp1")),
      """
        {
          "name":"dp1"
        }
      """)

    "fail when the name length is too long" ! expectedErrorTest[CreateDemandPartnerRequest](
      """
    {
      "name":"spqasdlklkdfngklfajglkajgldsjglkjadsgkljsdglkaslvasdlvkladsvkladallskdfjlasdjfkldsajfkldsajfkldsjfkljdskfjdkslfjdsfkjdsfldjflkdsjfkldsjaljsdlkfjdslkfjdklsfjkldsjfkldsjfkljdskfljdslkfjkdlsfjdsljfdsfkljdslkfjlsajflkdsjfkldsjfldsjfkldsjfkldsjfklsdjflkdsjflksdjflksdjfkdsljfkdsjfld"
    }

    """,
      JsError(
        __ \ "name",
        DemandPartnerName.LengthError))
  }

  "CreateSupplyPartnerRequests" should {

    def genCreateSupplyPartnerRequest: Gen[CreateSupplyPartnerRequest] =
      genSupplyPartnerName.map(CreateSupplyPartnerRequest.apply)
    implicit def arbCreateSupplyPartnerRequests: Arbitrary[CreateSupplyPartnerRequest] =
      Arbitrary(genCreateSupplyPartnerRequest)

    "CreateSupplyPartnerRequest.Equal" ! ScalazProperties.equal.laws[CreateSupplyPartnerRequest]

    "Format[CreateSupplyPartnerRequest]" ! checkFormat[CreateSupplyPartnerRequest]

    "Parse valid input correctly" ! expectedFormatTest(
      CreateSupplyPartnerRequest(SupplyPartnerName("sp1")),
      """
        {
          "name":"sp1"

        }
      """)

    "fail when the name length is too long" ! expectedErrorTest[CreateSupplyPartnerRequest](
      """
        {
          "name":"spqasdlklkdfngklfajglkajgldsjglkjadsgkljsdglkaslvasdlvkladsvkladallskdfjlasdjfkldsajfkldsajfkldsjfkljdskfjdkslfjdsfkjdsfldjflkdsjfkldsjaljsdlkfjdslkfjdklsfjkldsjfkldsjfkljdskfljdslkfjkdlsfjdsljfdsfkljdslkfjlsajflkdsjfkldsjfldsjfkldsjfkldsjfklsdjflkdsjflksdjflksdjfkdsljfkdsjfld",
          "spTypes":[
            "mobile"
          ]
        }
      """,
      JsError(
        __ \ "name",
        SupplyPartnerName.LengthError))
  }
}
