package com.ntoggle.kubitschek
package api

import com.ntoggle.albi._
import com.ntoggle.albi.AlbiGenerators._
import com.ntoggle.goldengate.NGen
import com.ntoggle.goldengate.playjson.test.JsonTests
import com.ntoggle.kubitschek.Tools.JsonTestsAddition
import com.ntoggle.kubitschek.domain.{ConfigurationEndpoint, DemandPartnerMaxQps}
import com.ntoggle.kubitschek.domain.DomainGenerators._
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers
import play.api.data.validation.ValidationError
import play.api.libs.json._

import scalaz.syntax.apply._
import scalaz.scalacheck.ScalaCheckBinding._
import scalaz.scalacheck.ScalazProperties


class RouterConfigurationSpec extends Specification with ScalaCheck with ScalazMatchers with JsonTests with JsonTestsAddition {
  def genPort: Gen[Port] = Gen.choose(1, 65535).map(Port.apply)
  def genConfigurationEndpoint: Gen[ConfigurationEndpoint] = ^(
    for {
      a <- Gen.choose(0, 255).map(_.toString)
      b <- Gen.choose(0, 255).map(_.toString)
      c <- Gen.choose(0, 255).map(_.toString)
      d <- Gen.choose(0, 255).map(_.toString)
    } yield a + "." + b + "." + c + "." + d, genPort)(ConfigurationEndpoint.apply)

  "RouterConfigurationRequest" should {
    def genRouterConfigurationRequest: Gen[RouterConfigurationRequest] =
      ^(genConfigurationEndpoint,genDemandPartnerMaxQps)(RouterConfigurationRequest.apply)
    implicit def arbRouterConfigurationRequest: Arbitrary[RouterConfigurationRequest] =
      Arbitrary(genRouterConfigurationRequest)

    "RouterConfigurationRequest.Equal" ! ScalazProperties.equal.laws[RouterConfigurationRequest]

    "Format[RouterConfigurationRequest]" ! checkFormat[RouterConfigurationRequest]

    "Parse valid input correctly" ! expectedFormatTest(
      RouterConfigurationRequest(
        ConfigurationEndpoint("c-63-345-34-54.hsd1.co.ntoggle.com", Port(7000)),
        DemandPartnerMaxQps(MaxQps(23400))),
      """
        {
          "maxQps" : 23400,
          "configEndpoint": {
            "host": "c-63-345-34-54.hsd1.co.ntoggle.com",
            "port":7000
          }
        }
      """
    )
    "Request with invalid port causes JsFailure" ! expectedErrorTest[RouterConfigurationRequest](
      """
      {
        "maxQps" : 23400,
        "configEndpoint": {
          "host": "c-63-345-34-54.hsd1.co.ntoggle.com",
          "port":-1
        }
      }
    """,
      JsError(
        Seq(
          ((__ \ "configEndpoint") \ "port") -> Seq(Port.InvalidPortError))))

    "Request with invalid maxqps causes failure" ! expectedErrorTest[RouterConfigurationRequest](
      """
      {
        "maxQps" : -1,
        "configEndpoint": {
          "host": "c-63-345-34-54.hsd1.co.ntoggle.com",
          "port":100
        }
      }
     """,
      JsError(
        __ \ "maxQps",
        MaxQps.validationError(-1)))

    "Request with invalid host causes failure" ! expectedErrorTest[RouterConfigurationRequest](
      """
    {
      "maxQps" : 10000,
      "configEndpoint": {
        "host": "string-",
        "port":100
      }
    }
  """,
      JsError(
        (__ \ "configEndpoint") \ "host",
        ConfigurationEndpoint.InvalidHostError))
  }

  "RouterConfigurationUpdateRequest" should {
    def genRouterConfigurationUpdateRequest: Gen[RouterConfigurationUpdateRequest] =
      genConfigurationEndpoint.map(RouterConfigurationUpdateRequest.apply)
    implicit def arbRouterConfigurationUpdateRequest: Arbitrary[RouterConfigurationUpdateRequest] =
      Arbitrary(genRouterConfigurationUpdateRequest)

    "RouterConfigurationUpdateRequest.Equal" ! ScalazProperties.equal.laws[RouterConfigurationUpdateRequest]

    "Format[RouterConfigurationUpdateRequest]" ! checkFormat[RouterConfigurationUpdateRequest]

    "Parse valid input correctly" ! expectedFormatTest(
      RouterConfigurationUpdateRequest(ConfigurationEndpoint("c-63-345-34-54.hsd1.co.ntoggle.com", Port(7000))),
      """
      {
        "configEndpoint": {
          "host": "c-63-345-34-54.hsd1.co.ntoggle.com",
          "port":7000
        }
      }
     """)

    "Request with invalid port causes JsFailure" ! expectedErrorTest[RouterConfigurationUpdateRequest](
      """
      {
        "configEndpoint": {
          "host": "c-63-345-34-53.hsd1.co.ntoggle.com",
          "port":-1
        }
      }
    """,
      JsError(
        Seq(
          ((__ \ "configEndpoint") \ "port") -> Seq(Port.InvalidPortError))))

    "Request with invalid host causes failure" ! expectedErrorTest[RouterConfigurationUpdateRequest](
      """
        {
          "configEndpoint": {
            "host": "string-",
            "port":100
          }
        }
      """,
      JsError(
        (__ \ "configEndpoint") \ "host",
        ConfigurationEndpoint.InvalidHostError))
  }

  "RouterConfigurationResponse" should {
    def genRouterConfigurationResponse: Gen[RouterConfigurationResponse] =
      ^^(genDemandPartnerId,
        genSupplyPartnerId,
        genConfigurationEndpoint)(RouterConfigurationResponse.apply)
    implicit def arbRouterConfigurationResponse: Arbitrary[RouterConfigurationResponse] =
      Arbitrary(genRouterConfigurationResponse)

    "RouterConfigurationResponse.Equal" ! ScalazProperties.equal.laws[RouterConfigurationResponse]

    "Format[RouterConfigurationResponse]" ! checkFormat[RouterConfigurationResponse]

    "Parse valid input correctly" ! expectedFormatTest(
      RouterConfigurationResponse(
        DemandPartnerId("de305d54-75b4-431b-adb2-eb6b9e546014"),
        SupplyPartnerId("de305d54-75b4-431b-adb2-eb6b9e546014"),
        ConfigurationEndpoint("c-63-345-34-54.hsd1.co.ntoggle.com", Port(7000))
      ),
      """
      {
        "dpId": "de305d54-75b4-431b-adb2-eb6b9e546014",
        "spId": "de305d54-75b4-431b-adb2-eb6b9e546014",
        "configEndpoint": {
          "host": "c-63-345-34-54.hsd1.co.ntoggle.com",
          "port":7000
        }
      }
     """)

    "Request with invalid port causes JsFailure" ! expectedErrorTest[RouterConfigurationResponse](
      """
      {
        "dpId": "de305d54-75b4-431b-adb2-eb6b9e546014",
        "spId": "de305d54-75b4-431b-adb2-eb6b9e546014",
        "spIdType": "mobile",
        "configEndpoint": {
          "host": "c-63-345-34-54.hsd1.co.ntoggle.com",
          "port":9999999
        }
      }
     """,
      JsError(
        Seq(
          ((__ \ "configEndpoint") \ "port") -> Seq(Port.InvalidPortError))))

    "Request with invalid host causes failure" ! expectedErrorTest[RouterConfigurationResponse](
        """
        {
          "dpId": "de305d54-75b4-431b-adb2-eb6b9e546014",
          "spId": "de305d54-75b4-431b-adb2-eb6b9e546014",
          "spIdType": "mobile",
          "configEndpoint": {
            "host": "string-",
            "port":700
          }
        }
       """,
        JsError(
          (__ \ "configEndpoint") \ "host",
          ConfigurationEndpoint.InvalidHostError))

    "Request with invalid dpId causes failure" ! expectedErrorTest[RouterConfigurationResponse](
        """
        {
          "dpId": "de305d54-75b424342eb6b9e546014",
          "spId": "de305d54-75b4-431b-adb2-eb6b9e546014",
          "spIdType": "mobile",
          "configEndpoint": {
            "host": "c-63-345-34-54.hsd1.co.ntoggle.com",
            "port":700
          }
        }
       """,
        JsError(
          __ \ "dpId",
          "error.expected.uuid"))
  }
}