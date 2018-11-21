package com.ntoggle.kubitschek.domainpersistence.sql

import com.ntoggle.albi.TrafficType
import com.ntoggle.kubitschek.domain.{RuleStatus, RuleCreatedInstant, RuleName, RuleId}

case class SqlRule(
  id: RuleId,
  name: RuleName,
  created: RuleCreatedInstant,
  trafficType: TrafficType) {

}
