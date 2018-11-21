package com.ntoggle.kubitschek
package domainpersistence
package sql

private[sql] case class SqlRuleConditionException(
  ruleConditionId: SqlRuleConditionId,
  value: String)
