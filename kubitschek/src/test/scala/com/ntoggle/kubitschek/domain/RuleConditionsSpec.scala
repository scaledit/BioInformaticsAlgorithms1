package com.ntoggle.kubitschek
package domain

import com.ntoggle.goldengate.playjson.test.JsonTests
import org.scalacheck.Arbitrary
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers

class RuleConditionsSpec
  extends Specification
  with ScalaCheck
  with JsonTests
  with ScalazMatchers {
  import DomainGenerators._

  implicit val arbRuleConditions: Arbitrary[RuleConditions] =
    Arbitrary(genRuleConditions)

  "RuleConditions.format" ! checkFormat[RuleConditions]
}
