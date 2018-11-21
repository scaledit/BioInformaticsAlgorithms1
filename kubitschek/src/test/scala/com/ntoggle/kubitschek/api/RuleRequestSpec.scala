package com.ntoggle.kubitschek
package api

import com.ntoggle.albi._
import com.ntoggle.albi.devices._
import com.ntoggle.goldengate.playjson.test.JsonTests
import com.ntoggle.kubitschek.Tools.JsonTestsAddition
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.domain.DomainGenerators._
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers
import play.api.libs.json._
import scalaz.syntax.apply._
import scalaz.scalacheck.ScalaCheckBinding._
import scalaz.scalacheck.ScalazProperties

class RuleRequestSpec extends Specification with ScalaCheck with ScalazMatchers with JsonTests with JsonTestsAddition {

  def genCreateRuleRequest: Gen[CreateRuleRequest] =
    ^^(genRuleName, Gen.oneOf(Mobile, Desktop), genRuleConditions)(CreateRuleRequest.apply)
  implicit def arbCreateRequest: Arbitrary[CreateRuleRequest] =
    Arbitrary(genCreateRuleRequest)

  "CreateRuleRequest" should {

    "CreateRuleRequest.Equal" ! ScalazProperties.equal.laws[CreateRuleRequest]

    "Format[CreateRuleRequest]" ! checkFormat[CreateRuleRequest]

    "Format[CreateRuleRequest] format test" ! expectedFormatTest(
      CreateRuleRequest(
        RuleName("rule1"),
        Mobile,
        RuleConditions(
          Some(RuleCondition[CarrierId](
            DefaultConditionAction.Allow,
            Set[CarrierId](CarrierId("AT&T"), CarrierId("Verizon")),
            UndefinedConditionAction.Allow)),
          None, None, None, None, None, None, None, None, None, None, None, None,
          None, None, None ,None, None, None)),
      """
    {
      "name":"rule1",
      "trafficType": "mobile",
      "conditions": {
        "carrier": {
          "default": "allow",
          "undefined": "allow",
          "exceptions": [
            "AT&T",
            "Verizon"
          ]
        }
      }
    }
    """)

    "fail when default type is incorrect" ! expectedErrorTest[CreateRuleRequest](
      """
      {
        "name":"rule1",
        "trafficType": "mobile",
        "conditions": {
          "carrier": {
            "default": "FAIL",
            "undefined": "allow",
            "exceptions": [
              "AT&T",
              "Verizon"
            ]
          }
        }
      }
      """,
      JsError(__ \ "conditions" \ "carrier" \ "default",
        "Invalid default condition status"))

    "fail when undefined type is incorrect" ! expectedErrorTest[CreateRuleRequest](
      """
      {
        "name":"rule1",
        "trafficType": "mobile",
        "conditions": {
          "carrier": {
            "default": "allow",
            "undefined": "FAIL",
            "exceptions": [
              "AT&T",
              "Verizon"
            ]
          }
        }
      }
      """,
      JsError(
        __ \ "conditions" \ "carrier" \ "undefined",
        "Invalid undefined condition action")

    )
  }

  def genReplaceRuleRequest: Gen[ReplaceRuleRequest] =
    ^^(genRuleName, Gen.oneOf(Mobile, Desktop), genRuleConditions)(ReplaceRuleRequest.apply)
  implicit def arbModifyRequest: Arbitrary[ReplaceRuleRequest] =
    Arbitrary(genReplaceRuleRequest)

  "ReplaceRuleRequest" should {
    "ReplaceRuleRequest.Equal" ! ScalazProperties.equal.laws[ReplaceRuleRequest]
    "Format[ReplaceRuleRequest]" ! checkFormat[ReplaceRuleRequest]
    "Format[ReplaceRuleRequest] format test" ! expectedFormatTest(
      ReplaceRuleRequest(
        RuleName("rule1"),
        Mobile,
        RuleConditions(
          Option(
            RuleCondition[CarrierId](
              DefaultConditionAction.Allow,
              Set[CarrierId](CarrierId("AT&T"), CarrierId("Verizon")),
              UndefinedConditionAction.Allow)),
          Option(
            RuleCondition(
              DefaultConditionAction.Exclude,
              Set(HandsetId(
                HandsetManufacturer("Some-MFG"),
                HandsetModel("Some-Model"))),
              UndefinedConditionAction.Allow)),
          Option(
            RuleCondition(
              DefaultConditionAction.Allow,
              Set(NoDeviceId),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Exclude,
              Set(OsId(
                OsName("Some-OS"),
                OsVersion("Some-Version"))),
              UndefinedConditionAction.Allow)),
          Option(
            RuleCondition(
              DefaultConditionAction.Exclude,
              Set(Wifi),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Exclude,
              Set(HasIpAddress),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Allow,
              Set(AppId("e5276ba5-2746-12b5-c64a-1112a5d5c421")),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Allow,
              Set(IsBanner),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Allow,
              Set(IsMraid),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Allow,
              Set(IsInterstitial),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Allow,
              Set(IsVideo),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Allow,
              Set(IsNative),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Exclude,
              Set(AdSizeId(AdSizeWidth(1024), AdSizeHeight(768))),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Exclude,
              Set(com.ntoggle.kubitschek.domain.LatLong),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Allow,
              Set(CountryIdAlpha2("US")),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Exclude,
              Set(RegionId(RegionCode("FL"), CountryIdAlpha2("US"))),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Allow,
              Set(CityName(s"Some-City∆¨ˆºµ≈∫Ω")),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Allow,
              Set(ZipAlpha(s"02114")),
              UndefinedConditionAction.Exclude)),
          Option(
            RuleCondition(
              DefaultConditionAction.Allow,
              Set(UserListName(s"auto-something")),
              UndefinedConditionAction.Exclude)))),
      """
    {
      "name":"rule1",
      "trafficType": "mobile",
      "conditions": {
        "carrier": {
          "default": "allow",
          "undefined": "allow",
          "exceptions": ["AT&T", "Verizon"]
        },
        "handset": {
          "default": "exclude",
          "undefined": "allow",
          "exceptions": ["F536F6D652D4D4647-F536F6D652D4D6F64656C"]
        },
        "deviceIdExistence": {
          "default": "allow",
          "undefined": "exclude",
          "exceptions": [false]
        },
          "os": {
          "default": "exclude",
          "undefined": "allow",
          "exceptions": ["F536F6D652D4F53-F536F6D652D56657273696F6E"]
        },
        "wifi": {
          "default": "exclude",
          "undefined": "exclude",
          "exceptions": [true]
        },
        "ipAddressExistence": {
          "default": "exclude",
          "undefined": "exclude",
          "exceptions": [true]
        },
        "appAndSite": {
          "default": "allow",
          "undefined": "exclude",
          "exceptions": ["e5276ba5-2746-12b5-c64a-1112a5d5c421"]
        },
        "banner": {
          "default": "allow",
          "undefined": "exclude",
          "exceptions": [true]
        },
        "mraid": {
          "default": "allow",
          "undefined": "exclude",
          "exceptions": [true]
        },
        "interstitial": {
          "default": "allow",
          "undefined": "exclude",
          "exceptions": [true]
        },
        "video": {
          "default": "allow",
          "undefined": "exclude",
          "exceptions": [true]
        },
        "native": {
          "default": "allow",
          "undefined": "exclude",
          "exceptions": [true]
        },
        "adSize": {
          "default": "exclude",
          "undefined": "exclude",
          "exceptions": ["1024x768"]
        },
        "latlong": {
          "default": "exclude",
          "undefined": "exclude",
          "exceptions": [true]
        },
        "country": {
          "default": "allow",
          "undefined": "exclude",
          "exceptions": ["US"]
        },
        "region": {
          "default": "exclude",
          "undefined": "exclude",
          "exceptions": ["US-FL"]
        },
        "city": {
          "default": "allow",
          "undefined": "exclude",
          "exceptions": ["Some-City∆¨ˆºµ≈∫Ω"]
        },
        "zip": {
          "default": "allow",
          "undefined": "exclude",
          "exceptions": ["02114"]
        },
        "userList" : {
        "default": "allow",
        "undefined": "exclude",
        "exceptions": ["auto-something"]
        }
      }
    }
    """
    )

    "fail when default type is incorrect" ! expectedErrorTest[ReplaceRuleRequest](
      """
      {
        "id":"d5272b40-57d6-11e5-a74a-0002a5d5c51b",
        "name":"rule1",
        "trafficType":"mobile",
        "conditions": {
          "carrier": {
            "default": "FAIL",
            "undefined": "allow",
            "exceptions": [
              "AT&T",
              "Verizon"
            ]
          }
        }
      }
      """,
      JsError(
        __ \ "conditions" \ "carrier" \ "default",
        "Invalid default condition status"))

    "fail when undefined type is incorrect" ! expectedErrorTest[ReplaceRuleRequest](
      """
      {
        "id":"d5272b40-57d6-11e5-a74a-0002a5d5c51b",
        "name":"rule1",
        "trafficType":"mobile",
        "conditions": {
          "carrier": {
            "default": "allow",
            "undefined": "FAIL",
            "exceptions": [
              "AT&T",
              "Verizon"
            ]
          }
        }
      }
      """,
      JsError(
        __ \ "conditions" \ "carrier" \ "undefined",
        "Invalid undefined condition action"))
  }
}
