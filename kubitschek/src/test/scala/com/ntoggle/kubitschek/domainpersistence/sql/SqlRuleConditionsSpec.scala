package com.ntoggle.kubitschek
package domainpersistence
package sql

import java.util.UUID
import java.util.concurrent.TimeUnit

import com.ntoggle.albi._
import com.ntoggle.goldengate.NGen
import com.ntoggle.goldengate.Syntax._
import com.ntoggle.goldengate.concurrent.ScalaFuture
import com.ntoggle.kubitschek.domain.{RuleId, RuleConditions, DomainGenerators}
import com.ntoggle.kubitschek.domainpersistence.sql.SqlRuleConditions.SqlConnectionType
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scalaz.Foldable
import scalaz.std.option._
import scalaz.std.list._
import scalaz.std.scalaFuture._
import scalaz.syntax.equal._
import scalaz.syntax.traverse._

class SqlRuleConditionsSpec
  extends Specification
  with ScalaCheck
  with ScalazMatchers {
  import AlbiGenerators._
  import DomainGenerators._
  import SqlDomainGenerators._

  private val timeout = Duration(2, TimeUnit.SECONDS)

  "SqlRuleConditions.SqlConnectionType.toStringKey/fromStringKey" !
    Prop.forAll(genConnectionType) {
      expected =>
        val actual = SqlRuleConditions.SqlConnectionType.fromStringKey(
          SqlRuleConditions.SqlConnectionType.toStringKey(expected))
        actual must equal(Option(expected))
    }

  val fromStringKeyInput: Gen[(String, Option[ConnectionType])] = Gen.oneOf(
    Gen.const(SqlConnectionType.Keys.Wifi -> Option(Wifi)),
    Gen.const(SqlConnectionType.Keys.NotWifi -> Option(NotWifi)),
    Arbitrary.arbitrary[String].map(_ -> Option.empty))
  "SqlRuleConditions.SqlConnectionType.fromStringKey" !
    Prop.forAll(fromStringKeyInput) {
      case (input, expected) =>
        val actual = SqlConnectionType.fromStringKey(input)
        actual must equal(expected)
    }

  val newId = () =>
    Future.successful(SqlRuleConditionId(UUID.randomUUID().toString))

  "fromRuleConditions/toRuleConditions" ! Prop.forAll(genRuleId, genRuleConditions) {
    (ruleId, conditions) =>
      val expected =
        if (conditions =/= RuleConditions.Empty) Map(ruleId -> conditions)
        else Map.empty[RuleId, RuleConditions]
      ScalaFuture.await(timeout) {
        for {
          sql <- SqlRuleConditions.fromRuleConditions(
            ruleId,
            conditions,
            newId)
          actual = SqlRuleConditions.toRuleConditions(sql)
        } yield actual ==== expected
      }
  }

  "fromRuleConditions/toRuleConditions with empty ruleConditions" ! {
    val ruleId = RuleId("98eec99a-afab-4e16-b575-785a40e4a32d")
    val conditions = RuleConditions.Empty
    val expected = Map.empty[RuleId, RuleConditions]
    ScalaFuture.await(timeout) {
      for {
        sql <- SqlRuleConditions.fromRuleConditions(
          ruleId,
          conditions,
          newId)
        actual = SqlRuleConditions.toRuleConditions(sql)
      } yield actual ==== expected
    }
  }


  val listOfRules = {
    NGen.containerOfSizeRanged[List, (RuleId, RuleConditions)](
      0,
      5,
      Gen.zip(
        genRuleId,
        genRuleConditions)).map(_.distinctBy(_._1))
  }
  "fromRuleConditions/toRuleConditions can handle multiple rules" !
    Prop.forAllNoShrink(listOfRules) {
      input =>
        import scalaz.syntax.equal._
        // Empty rule conditions result in nothing to insert,
        // so do not expect them
        val expected = input.filter(_._2 =/= RuleConditions.Empty).toMap
        ScalaFuture.await(timeout) {
          for {
            sql <- input.traverse[Future, SqlRuleConditions] {
              case (ruleId, conditions) =>
                SqlRuleConditions.fromRuleConditions(
                  ruleId,
                  conditions,
                  newId)
            }.map(Foldable[List].fold[SqlRuleConditions])
            actual = SqlRuleConditions.toRuleConditions(sql)
          } yield actual ==== expected
        }
    }

  "fromRuleConditions with empty rule conditions results in empty sql rule conditions" ! {
    val id = RuleId("873546b8-85b6-4879-ba86-2d8509dbf349")
    val conditions = RuleConditions.Empty
    ScalaFuture.await(timeout) {
      for {
        sql <- SqlRuleConditions.fromRuleConditions(id, conditions, newId)
      } yield sql must equal(SqlRuleConditions.Empty)
    }
  }
}
