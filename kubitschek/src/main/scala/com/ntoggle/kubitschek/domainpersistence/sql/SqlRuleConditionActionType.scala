package com.ntoggle.kubitschek
package domainpersistence
package sql

import com.ntoggle.goldengate.scalazint.EqualzAndShowz
import com.ntoggle.kubitschek.domain._
import scalaz.syntax.equal._
import scalaz.std.anyVal.intInstance

sealed trait SqlRuleConditionActionType
object SqlRuleConditionActionType extends EqualzAndShowz[SqlRuleConditionActionType] {
  val Allow: SqlRuleConditionActionType = AllowSqlRuleConditionActionType
  val Block: SqlRuleConditionActionType = BlockSqlRuleConditionActionType
  object Keys {
    val Block = 0
    val Allow = 1
  }
  def fromInt(i: Int): SqlRuleConditionActionType =
    if (i === Keys.Block) Block else Allow
  def toInt(action: SqlRuleConditionActionType): Int = action match {
    case AllowSqlRuleConditionActionType => Keys.Allow
    case BlockSqlRuleConditionActionType => Keys.Block
  }

  val fromUndefined: UndefinedConditionAction => SqlRuleConditionActionType = {
    case AllowUndefinedConditionAction => Allow
    case ExcludeUndefinedConditionAction => Block
  }
  val toUndefined: SqlRuleConditionActionType => UndefinedConditionAction = {
    case AllowSqlRuleConditionActionType => AllowUndefinedConditionAction
    case BlockSqlRuleConditionActionType => ExcludeUndefinedConditionAction
  }

  val fromDefault: DefaultConditionAction => SqlRuleConditionActionType = {
    case AllowAllDefaultConditionAction => Allow
    case ExcludeAllDefaultConditionAction => Block
  }
  val toDefault: SqlRuleConditionActionType => DefaultConditionAction = {
    case AllowSqlRuleConditionActionType => AllowAllDefaultConditionAction
    case BlockSqlRuleConditionActionType => ExcludeAllDefaultConditionAction
  }
}
case object AllowSqlRuleConditionActionType extends SqlRuleConditionActionType
case object BlockSqlRuleConditionActionType extends SqlRuleConditionActionType
