package com.ntoggle.kubitschek
package service

import com.ntoggle.albi._
import com.ntoggle.goldengate.NGen
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.services.ValidateService
import org.scalacheck.{Prop, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers
import scalaz.syntax.apply._
import scalaz.std.function._
import scalaz.std.list._

class ValidateServiceSpec
  extends Specification
  with ScalazMatchers
  with ScalaCheck {

  // KUB-53 These were identified cases which should validate successfully
  def genFailingCases: Gen[(String, OsId)] =
    Gen.oneOf(
      ("Android-8-800-5552", OsId(OsName("Android"), OsVersion("8-800-5552"))),
      ("Android-5.0-2", OsId(OsName("Android"), OsVersion("5.0-2"))),
      ("Android-4.4-", OsId(OsName("Android"), OsVersion("4.4-"))),
      ("Android-4.4-2", OsId(OsName("Android"), OsVersion("4.4-2"))),
      ("Android-20150622-LY", OsId(OsName("Android"), OsVersion("20150622-LY"))),
      ("Android-2.2-UPDATE1", OsId(OsName("Android"), OsVersion("2.2-UPDATE1"))))

  "validateOs with failing cases" ! Prop.forAllNoShrink(
    NGen.listOfMaxSize(
      5,
      genFailingCases)) { input =>

    val (strings, expected) =
      input.foldLeft((List.empty[String], List.empty[ParsedItem[OsId]])) {
        case ((xS, xEx), (x, osId)) =>
          (x :: xS, ParsedItem(osId, x) :: xEx)
      }
    val actual = ValidateService.validateOs(ValidateOsRequest(strings.reverse))
    actual must equal(ValidateOsResponse(List.empty, expected))
  }

  import AlbiGenerators._

  "validateOs" ! Prop.forAll(
    NGen.listOfMaxSize(
      20,
      genOsId.filter(// this is only specified to work when first item has no '-'
        !_.name.value.contains("-")))) { ids =>

    val idToString = (id: OsId) => s"${id.name.value}-${id.version.version}"
    val idToParsedItem = (id: OsId) => ParsedItem(id, idToString(id))
    val f = idToString tuple idToParsedItem

    val (strings, expected) = ids.map(f).unzip
    val actual = ValidateService.validateOs(ValidateOsRequest(strings.reverse))
    actual must equal(ValidateOsResponse(List.empty, expected))
  }

  "validateCountry" ! Prop.forAll(NGen.listOfMaxSize(20, genCountryIdAlpha2)) {
    ids =>
      val idToString = (id: CountryIdAlpha2) => s"${id.value}"
      val idToParsedItem = (id: CountryIdAlpha2) => ParsedItem(id, idToString(id))
      val f = idToString tuple idToParsedItem
      val (strings, expected) = ids.map(f).unzip
      val actual = ValidateService.validateCountry(ValidateCountryRequest(strings.reverse))
      actual must equal(ValidateCountryResponse(List.empty, expected))
  }

  // genRegionId won't generate a bad CountryIdAlpha2 and it comes first
  "validateRegion" ! Prop.forAll(NGen.listOfMaxSize(20, genRegionId)) {
    ids =>
      val idToString = (id: RegionId) => s"${id.country.value}-${id.code.value}"
      val idToParsedItem = (id: RegionId) => ParsedItem(id, idToString(id))
      val f = idToString tuple idToParsedItem

      val (strings, expected) = ids.map(f).unzip
      val actual = ValidateService.validateRegion(ValidateRegionRequest(strings.reverse))
      actual must equal(ValidateRegionResponse(List.empty, expected))
  }

  "validateHandset" ! Prop.forAll(
    NGen.listOfMaxSize(
      20,
      genHandsetId.filter(// this is only specified to work when first item has no '-'
        !_.manufacturer.value.contains("-")))) { ids =>

    val idToString = (id: HandsetId) => s"${id.manufacturer.value}-${id.handset.value}"
    val idToParsedItem = (id: HandsetId) => ParsedItem(id, idToString(id))
    val f = idToString tuple idToParsedItem

    val (strings, expected) = ids.map(f).unzip
    val actual = ValidateService.validateHandset(ValidateHandsetRequest(strings.reverse))
    actual must equal(ValidateHandsetResponse(List.empty, expected))
  }

}
