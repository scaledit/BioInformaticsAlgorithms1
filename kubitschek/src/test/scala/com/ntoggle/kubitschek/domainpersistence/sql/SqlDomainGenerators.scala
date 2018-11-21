package com.ntoggle.kubitschek
package domainpersistence
package sql

import org.scalacheck.{Arbitrary, Gen}

object SqlDomainGenerators {

  def genSqlRuleConditionActionType: Gen[SqlRuleConditionActionType] =
    Gen.oneOf(
      SqlRuleConditionActionType.Allow,
      SqlRuleConditionActionType.Block)

  implicit def arbSqlRuleConditionActionType: Arbitrary[SqlRuleConditionActionType] =
    Arbitrary(genSqlRuleConditionActionType)

}
