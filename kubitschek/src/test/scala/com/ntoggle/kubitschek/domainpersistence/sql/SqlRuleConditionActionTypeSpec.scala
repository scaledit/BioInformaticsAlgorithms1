package com.ntoggle.kubitschek
package domainpersistence
package sql

import com.ntoggle.goldengate.playjson.test.JsonTests
import com.ntoggle.kubitschek.domain.DomainGenerators
import org.scalacheck.Prop
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers

import scalaz.scalacheck.ScalazProperties

class SqlRuleConditionActionTypeSpec
  extends Specification
  with ScalaCheck
  with JsonTests
  with ScalazMatchers {
  import DomainGenerators._
  import SqlDomainGenerators._

  "SqlRuleConditionActionType.equal" !
    ScalazProperties.equal.laws[SqlRuleConditionActionType]

  "SqlRuleConditionActionType toInt/fromInt" !
    Prop.forAll(genSqlRuleConditionActionType) {
      expected =>
        val actual = SqlRuleConditionActionType.fromInt(
          SqlRuleConditionActionType.toInt(expected))
        actual must equal(expected)
    }

  "SqlRuleConditionActionType from/to UndefinedConditionAction" !
    Prop.forAll(genUndefinedConditionAction) {
      expected =>
        val actual = SqlRuleConditionActionType.toUndefined(
          SqlRuleConditionActionType.fromUndefined(expected))
        actual must equal(expected)
    }
  "SqlRuleConditionActionType to/from UndefinedConditionAction" !
    Prop.forAll(genSqlRuleConditionActionType) {
      expected =>
        val actual = SqlRuleConditionActionType.fromUndefined(
          SqlRuleConditionActionType.toUndefined(expected))
        actual must equal(expected)
    }

  "SqlRuleConditionActionType from/to DefaultConditionAction" !
    Prop.forAll(genDefaultConditionAction) {
      expected =>
        val actual = SqlRuleConditionActionType.toDefault(
          SqlRuleConditionActionType.fromDefault(expected))
        actual must equal(expected)
    }
  "SqlRuleConditionActionType to/from DefaultConditionAction" !
    Prop.forAll(genSqlRuleConditionActionType) {
      expected =>
        val actual = SqlRuleConditionActionType.fromDefault(
          SqlRuleConditionActionType.toDefault(expected))
        actual must equal(expected)
    }
}
