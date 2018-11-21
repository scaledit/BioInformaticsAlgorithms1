package com.ntoggle.kubitschek
package domain

import com.ntoggle.albi.DesiredToggledQps

case class VersionRule(
  versionRuleId: VersionRuleId,
  desiredQps: DesiredToggledQps)
