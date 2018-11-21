package com.ntoggle.kubitschek
package Tools

import com.ntoggle.goldengate.playjson.PlayJsResult._
import com.ntoggle.goldengate.playjson.PlayJsonUtils
import com.ntoggle.goldengate.playjson.test.JsonTests
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers
import play.api.libs.json._

import scalaz.{Show, Equal}


trait JsonTestsAddition extends JsonTests {
  this: Specification with ScalaCheck with ScalazMatchers =>

  def expectedReadsTest[A: Reads : Equal : Show](expectedObject: A, a: String) = {
    val actual = Json.parse(a).validate[A]
    actual must equal(PlayJsonUtils.jsResult(expectedObject))
  }

  def expectedFormatTest[A: Format : Equal : Show](expectedObject: A, expectedJson: String) =
    expectedWritesTest(expectedObject, expectedJson) and
      expectedReadsTest(expectedObject, expectedJson)

  def expectedErrorTest[A: Reads : Equal : Show](invalidJson: String, expectedError: JsResult[A]) = {
    val actual = Json.parse(invalidJson).validate[A]
    actual must equal(expectedError)
  }
}
