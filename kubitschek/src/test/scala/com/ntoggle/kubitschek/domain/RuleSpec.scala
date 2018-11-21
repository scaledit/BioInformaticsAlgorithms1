package com.ntoggle.kubitschek.domain

import com.ntoggle.goldengate.playjson.test.JsonTests
import org.scalacheck.Prop
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers
import scalaz.scalacheck.ScalazProperties
import scalaz.std.anyVal.booleanInstance

class RuleSpec
  extends Specification
  with ScalaCheck
  with JsonTests
  with ScalazMatchers {
  import DomainGenerators._

  "RuleStatus.equal" ! ScalazProperties.equal.laws[RuleStatus]
  "RuleStatus.toBoolean/fromBoolean" ! Prop.forAll(genRuleStatus) {
    expected =>
      val actual = RuleStatus.fromBoolean(RuleStatus.toBoolean(expected))
      actual must equal(expected)
  }
  "RuleStatus.fromBoolean/toBoolean" ! Prop.forAll {
    (expected: Boolean) =>
      val actual = RuleStatus.toBoolean(RuleStatus.fromBoolean(expected))
      actual must equal(expected)
  }

}
