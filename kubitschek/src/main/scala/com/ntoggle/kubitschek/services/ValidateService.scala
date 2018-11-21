package com.ntoggle.kubitschek
package services

import com.google.common.base.Splitter
import com.ntoggle.albi._
import com.ntoggle.kubitschek.api._
import monocle.Lens
import scala.collection.JavaConverters._
import scalaz.{-\/, \/}
import scalaz.std.option._
import scalaz.syntax.apply._
import scalaz.syntax.std.option._

object ValidateService {

  private val splitter = Splitter.on('-').limit(2)
  // KUB-53 - this splits string into two only and
  // will not work correctly if A contains split char!!!
  private def comboId[A, B, C](
    failed: String => FailedItem,
    fA: String => Option[A],
    fB: String => Option[B],
    fC: (A, B) => C)(input: String): FailedItem \/ ParsedItem[C] =
    splitter.split(input).asScala.toList match {
      case a :: b :: Nil =>
        ^(fA(a), fB(b))(fC)
          .\/>(failed(input))
          .map(ParsedItem(_, input))
      case _ => -\/(failed(input))
    }

  private def failureLens[A]: Lens[(List[FailedItem], List[A]), List[FailedItem]] =
    Lens.apply((_: (List[FailedItem], List[A]))._1)(a => t => (a, t._2))
  private def secondLens[A]: Lens[(List[FailedItem], List[A]), List[A]] =
    Lens.apply((_: (List[FailedItem], List[A]))._2)(a => t => (t._1, a))

  def validateOs(input: ValidateOsRequest): ValidateOsResponse = {
    val parsed = input.data.foldLeft(
      (List.empty[FailedItem], List.empty[ParsedItem[OsId]])) { (acc, i) =>
      comboId(
        FailedItem(_, "invalid OS"),
        OsName(_).some,
        OsVersion(_).some,
        OsId.apply)(i).fold(
        a => failureLens.modify(c => a :: c)(acc),
        a => secondLens[ParsedItem[OsId]].modify(a :: _)(acc))
    }
    ValidateOsResponse.apply _ tupled parsed
  }

  def validateCountry(input: ValidateCountryRequest): ValidateCountryResponse = {
    val parsed = input.data.foldLeft(
      (List.empty[FailedItem], List.empty[ParsedItem[CountryIdAlpha2]])) { (acc, i) =>
      CountryIdAlpha2.fromString(i)
        .some(a => secondLens[ParsedItem[CountryIdAlpha2]].modify(ParsedItem(a, i) :: _)(acc))
        .none(failureLens.modify(FailedItem(i, "invalid CountryIdAlpha2") :: _)(acc))
    }
    ValidateCountryResponse.apply _ tupled parsed
  }

  def validateRegion(input: ValidateRegionRequest): ValidateRegionResponse = {
    import scalaz.syntax.std.function2._ // for 'flip'
    val parsed = input.data.foldLeft(
      (List.empty[FailedItem], List.empty[ParsedItem[RegionId]])) { (acc, i) =>
      comboId(
        FailedItem(_, "invalid Region"),
        CountryIdAlpha2.fromString,
        RegionCode(_).some,
        (RegionId.apply _).flip // existing code had country expects country first in string
      )(i).fold(
        a => failureLens.modify(c => a :: c)(acc),
        a => secondLens[ParsedItem[RegionId]].modify(a :: _)(acc))
    }
    ValidateRegionResponse.apply _ tupled parsed
  }

  def validateHandset(input: ValidateHandsetRequest): ValidateHandsetResponse = {
    val parsed = input.data.foldLeft(
      (List.empty[FailedItem], List.empty[ParsedItem[HandsetId]])) { (acc, i) =>
      comboId(
        FailedItem(_, "invalid Handset"),
        HandsetManufacturer(_).some,
        HandsetModel(_).some,
        HandsetId.apply)(i).fold(
        a => failureLens.modify(c => a :: c)(acc),
        a => secondLens[ParsedItem[HandsetId]].modify(a :: _)(acc))
    }
    ValidateHandsetResponse.apply _ tupled parsed
  }
}
