package com.ntoggle.kubitschek.json

import com.ntoggle.goldengate.NGen
import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json._


/**
 * Generator of JsValue objects with a given tree depth
 * Adapted from spray.json.JsValueGenerators
 */
object JsValueGenerators {
  import Arbitrary.arbitrary
  import Gen._

  val parseableString: Gen[String] = Gen.someOf(('\u0020' to '\u007E').toVector).map(_.mkString)
  val genString: Gen[JsString] = parseableString.map(JsString(_))
  val genBoolean: Gen[JsBoolean] = oneOf(JsBoolean(false), JsBoolean(true))
  val genLongNumber: Gen[JsNumber] = arbitrary[Long].map(JsNumber(_))
  val genIntNumber: Gen[JsNumber] = arbitrary[Long].map(JsNumber(_))
  val genDoubleNumber: Gen[JsNumber] = arbitrary[Long].map(JsNumber(_))
  def genArray(depth: Int): Gen[JsArray] =
    if (depth == 0) JsArray()
    else
      for {
        n <- choose(0, 15)
        els <- Gen.containerOfN[List, JsValue](n, genValue(depth - 1))
      } yield JsArray(els.toVector)
  def genField(depth: Int): Gen[(String, JsValue)] =
    for {
      key <- parseableString
      value <- genValue(depth)
    } yield key -> value
  def genObject(depth: Int): Gen[JsObject] =
    if (depth == 0) JsObject(Nil)
    else {
      NGen.containerOfSizeRanged[Seq,(String, JsValue)](0, 10, genField(depth - 1))
        .map(JsObject.apply)
    }

  def genValue(depth: Int): Gen[JsValue] =
    oneOf(
      JsNull: Gen[JsValue],
      genString,
      genBoolean,
      genLongNumber,
      genDoubleNumber,
      genIntNumber,
      genArray(depth),
      genObject(depth))
  implicit val arbitraryValue: Arbitrary[JsValue] = Arbitrary(genValue(3))
}
