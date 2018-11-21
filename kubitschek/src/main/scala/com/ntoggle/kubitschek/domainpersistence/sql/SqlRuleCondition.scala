package com.ntoggle.kubitschek
package domainpersistence
package sql

import com.ntoggle.kubitschek.domain.RuleId

private[sql] case class SqlRuleCondition(
  id: SqlRuleConditionId,
  ruleId: RuleId,
  attributeType: SqlRuleConditionAttributeType,
  defaultAction: SqlRuleConditionActionType,
  undefinedAction: SqlRuleConditionActionType)
