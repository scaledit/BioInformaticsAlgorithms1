package com.ntoggle.kubitschek
package domain

import com.ntoggle.goldengate.playjson.PlayJsonUtils
import com.ntoggle.goldengate.playjson.PlayJsonSyntax._
import com.ntoggle.goldengate.playjson.PlayFormat._
import com.ntoggle.goldengate.scalazint.EqualzAndShowz
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.syntax.std.option._
import scalaz.{Functor, Lens, @>}

sealed trait UndefinedConditionAction
object UndefinedConditionAction
  extends EqualzAndShowz[UndefinedConditionAction] {
  val Allow: UndefinedConditionAction = AllowUndefinedConditionAction
  val Exclude: UndefinedConditionAction = ExcludeUndefinedConditionAction

  def fromStringValue(a: String): Option[UndefinedConditionAction] =
    a match {
      case "allow" => Some(UndefinedConditionAction.Allow)
      case "exclude" => Some(UndefinedConditionAction.Exclude)
      case _ => None
    }

  def toStringValue(a: UndefinedConditionAction): String =
    a match {
      case AllowUndefinedConditionAction => "allow"
      case ExcludeUndefinedConditionAction => "exclude"
    }

  implicit val format: Format[UndefinedConditionAction] = {

    val writes: Writes[UndefinedConditionAction] =
      implicitly[Writes[String]].contramap((s: UndefinedConditionAction) => toStringValue(s))

    val reads = Reads[UndefinedConditionAction] {
      case JsString(s) => fromStringValue(s).\/>("Invalid undefined condition action").toJsResult
      case other => PlayJsonUtils.jsError("Undefined condition action must be string")
    }

    Format(reads, writes)
  }
}
case object AllowUndefinedConditionAction extends UndefinedConditionAction
case object ExcludeUndefinedConditionAction extends UndefinedConditionAction

sealed trait DefaultConditionAction
object DefaultConditionAction extends EqualzAndShowz[DefaultConditionAction] {
  val Allow: DefaultConditionAction = AllowAllDefaultConditionAction
  val Exclude: DefaultConditionAction = ExcludeAllDefaultConditionAction

  def fromStringValue(s: String): Option[DefaultConditionAction] =
    s match {
      case "allow" => Some(DefaultConditionAction.Allow)
      case "exclude" => Some(DefaultConditionAction.Exclude)
      case _ => None
    }

  def toStringValue(a: DefaultConditionAction): String =
    a match {
      case AllowAllDefaultConditionAction => "allow"
      case ExcludeAllDefaultConditionAction => "exclude"
    }

  implicit val format: Format[DefaultConditionAction] = {

    val writes: Writes[DefaultConditionAction] =
      implicitly[Writes[String]].contramap((s: DefaultConditionAction) => toStringValue(s))

    val reads = Reads[DefaultConditionAction] {
      case JsString(s) => fromStringValue(s).\/>("Invalid default condition status").toJsResult
      case other => PlayJsonUtils.jsError("Default condition status must be string")
    }

    Format(reads, writes)
  }
}
case object AllowAllDefaultConditionAction extends DefaultConditionAction
case object ExcludeAllDefaultConditionAction extends DefaultConditionAction

case class RuleCondition[A](
  default: DefaultConditionAction,
  exceptions: Set[A],
  undefined: UndefinedConditionAction)
object RuleCondition {

  def defaultActionLens[A]: RuleCondition[A] @> DefaultConditionAction =
    Lens.lensu((rc, v) => rc.copy(default = v), _.default)

  def exceptionsLens[A]: RuleCondition[A] @> Set[A] =
    Lens.lensu((rc, v) => rc.copy(exceptions = v), _.exceptions)

  def undefinedActionLens[A]: RuleCondition[A] @> UndefinedConditionAction =
    Lens.lensu((rc, v) => rc.copy(undefined = v), _.undefined)

  def writes[A: Writes]: Writes[RuleCondition[A]] = (
    (__ \ "default").write[DefaultConditionAction] ~
      (__ \ "exceptions").write[List[A]].contramap[Set[A]](_.toList) ~
      (__ \ "undefined").write[UndefinedConditionAction]
    )(cond => (cond.default, cond.exceptions, cond.undefined))

  def reads[A: Reads]: Reads[RuleCondition[A]] = (
    (__ \ "default").read[DefaultConditionAction] ~
      (__ \ "exceptions").read[List[A]].map(_.toSet) ~
      (__ \ "undefined").read[UndefinedConditionAction]
    )(RuleCondition.apply[A] _)

  implicit def formats[A: Format]: Format[RuleCondition[A]] =
    Format[RuleCondition[A]](reads[A], writes[A])

  implicit val functorRuleCondition: Functor[RuleCondition] =
    new Functor[RuleCondition] {
      def map[A, B](fa: RuleCondition[A])(f: (A) => B): RuleCondition[B] =
        RuleCondition(
          fa.default,
          fa.exceptions.map(f),
          fa.undefined)
    }
}
