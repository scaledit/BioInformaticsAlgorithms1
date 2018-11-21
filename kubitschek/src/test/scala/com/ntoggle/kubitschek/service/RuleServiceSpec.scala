package com.ntoggle.kubitschek
package service

import java.util.UUID

import com.ntoggle.albi.{Mobile, DesiredToggledQps}
import com.ntoggle.kubitschek.api.{ReplaceRuleRequest, ReplaceRuleRequest$, CreateRuleRequest}
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.infra.{NotFoundRejection, BadRequestRejection}
import com.ntoggle.kubitschek.services.RuleService
import org.joda.time.Instant
import org.specs2.mutable.Specification
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scalaz.{-\/, \/-}
import scalaz.syntax.std.option._


class RuleServiceSpec extends Specification {

  "RuleService" should {
    val expectedvId = VersionId("vId")
    val expectedrId = RuleId("rId")
    val expectedRuleName = RuleName("rulename")
    val expectedTrafficType = Mobile
    val expectedCond = RuleConditions(None,
      Some(RuleCondition(AllowAllDefaultConditionAction, Set(), AllowUndefinedConditionAction)),
      None, None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None)
    val createRuleReq = CreateRuleRequest(expectedRuleName, expectedTrafficType, expectedCond)
    val replaceRuleReq = ReplaceRuleRequest(expectedRuleName, expectedTrafficType, expectedCond)

    "Create rule returns correctly" ! {
      def createRule = RuleService.save(
        () => Future.successful(UUID.randomUUID().toString),
        (vrId: VersionId, nr: Rule, qps: DesiredToggledQps) => Future.successful(
          Some(nr).\/>(ConfigurationError.generalError("ERROR"))))
      val fe = Await.result(createRule(expectedvId, createRuleReq).run, 1000.milli)
      val re = fe.getOrElse(RuleService.toGetRuleResponse(Rule(
        RuleId("Error"),
        RuleName("Error"),
        RuleCreatedInstant(Instant.now()),
        Mobile,
        RuleConditions.Empty)))
      re.name ==== expectedRuleName
      re.conditions ==== RuleService.toRuleConditionsResponse(expectedCond)
    }
    "Create rule fails when version does not exist" ! {
      def createRule = RuleService.save(
        () => Future.successful(UUID.randomUUID().toString),
        (vId: VersionId, nr: Rule, qps: DesiredToggledQps) => Future.successful(
          -\/(ConfigurationError.versionNotFound(vId))))
      val fe = Await.result(createRule(expectedvId, createRuleReq).run, 1000.milli)
      fe ==== -\/(NotFoundRejection("version not found", Some(Json.toJson(expectedvId))))
    }
    "Create rule fails when RuleId already exists" ! {
      def createRule = RuleService.save(
        () => Future.successful("Bad RuleId"),
        (vId: VersionId, nr: Rule, qps: DesiredToggledQps) => Future.successful(
          -\/(ConfigurationError.ruleAlreadyExists(nr.id))))
      val fe = Await.result(createRule(expectedvId, createRuleReq).run, 1000.milli)
      fe ==== -\/(BadRequestRejection("rule already exists", Some(Json.toJson(RuleId("Bad RuleId")))))
    }
    "Create rule fails when reach max rule capacity" ! {
      def createRule = RuleService.save(
        () => Future.successful(UUID.randomUUID().toString),
        (vId: VersionId, nr: Rule, qps: DesiredToggledQps) => Future.successful(
          -\/(ConfigurationError.maximumRulesReached(vId))))
      val fe = Await.result(createRule(expectedvId, createRuleReq).run, 1000.milli)
      fe ==== -\/(BadRequestRejection("maximum amount of rules reached", Some(Json.toJson(expectedvId))))
    }
    "Get rule returns correctly" ! {
      val nr = Rule(
        expectedrId,
        expectedRuleName,
        RuleCreatedInstant(Instant.now()),
        expectedTrafficType,
        expectedCond)
      def getRule = RuleService.get(
        (rId: RuleId) => Future.successful(
          Some(nr)))
      val fe = Await.result(getRule(expectedrId), 1000.milli)
      val re = fe.getOrElse(RuleService.toGetRuleResponse(
        Rule(RuleId("Error"),
          RuleName("Error"),
          RuleCreatedInstant(Instant.now()),
          expectedTrafficType,
          RuleConditions.Empty)))
      re.name ==== expectedRuleName
      re.conditions ==== RuleService.toRuleConditionsResponse(expectedCond)
    }
    "Get rule fails when rule does not exist" ! {
      val nr = Rule(
        expectedrId,
        expectedRuleName,
        RuleCreatedInstant(Instant.now()),
        expectedTrafficType,
        expectedCond)
      def getRule = RuleService.get(
        (rId: RuleId) => Future.successful(
          None))
      val fe = Await.result(getRule(expectedrId), 1000.milli)
      val re = fe.getOrElse(RuleService.toGetRuleResponse(Rule(
        RuleId("Error"),
        RuleName("Error"),
        RuleCreatedInstant(Instant.now()),
        expectedTrafficType,
        RuleConditions.Empty)))
      fe ==== None
    }

    "Replace rule returns correctly" ! {
      def replaceRule = RuleService.replace(
        () => Future.successful(UUID.randomUUID().toString),
        (vrId: VersionRuleId, nr: Rule) => Future.successful(
          Some(nr).\/>(ConfigurationError.generalError("ERROR"))))
      val fe = Await.result(replaceRule(expectedvId, expectedrId, replaceRuleReq).run, 1000.milli)
      val re = fe.getOrElse(RuleService.toGetRuleResponse(Rule(
        RuleId("Error"),
        RuleName("Error"),
        RuleCreatedInstant(Instant.now()),
        expectedTrafficType,
        RuleConditions.Empty)))
      re.name ==== expectedRuleName
      re.conditions ==== RuleService.toRuleConditionsResponse(expectedCond)
    }
    "Replace rule fails when version does not exist" ! {
      def replaceRule = RuleService.replace(
        () => Future.successful(UUID.randomUUID().toString),
        (vrId: VersionRuleId, nr: Rule) => Future.successful(
          -\/(ConfigurationError.versionNotFound(vrId.versionId))))
      val fe = Await.result(replaceRule(expectedvId, expectedrId, replaceRuleReq).run, 1000.milli)
      fe ==== -\/(NotFoundRejection("version not found", Some(Json.toJson(expectedvId))))
    }
    "Replace rule fails when rule does not exist" ! {
      def replaceRule = RuleService.replace(
        () => Future.successful(UUID.randomUUID().toString),
        (vrId: VersionRuleId, nr: Rule) => Future.successful(
          -\/(ConfigurationError.ruleNotFound(vrId.ruleId))))
      val fe = Await.result(replaceRule(expectedvId, expectedrId, replaceRuleReq).run, 1000.milli)
      fe ==== -\/(NotFoundRejection("rule not found", Some(Json.toJson(expectedrId))))
    }
    "Replace rule fails when rule already exist" ! {
      def replaceRule = RuleService.replace(
        () => Future.successful("Bad RuleId"),
        (vrId: VersionRuleId, nr: Rule) => Future.successful(
          -\/(ConfigurationError.ruleAlreadyExists(nr.id))))
      val fe = Await.result(replaceRule(expectedvId, expectedrId, replaceRuleReq).run, 1000.milli)
      fe ==== -\/(BadRequestRejection("rule already exists", Some(Json.toJson(RuleId("Bad RuleId")))))
    }

    "Remove rule returns correctly" ! {
      def removeRule = RuleService.remove(
        (vrId: VersionRuleId) => Future.successful(
          \/-(())))
      val fe = Await.result(removeRule(VersionRuleId(expectedvId, expectedrId)).run, 1000.milli)
      fe ==== \/-((()))
    }
    //    TODO BETA-701
    "Remove rule returns fails when version does not exist" ! {
      def removeRule = RuleService.remove(
        (vrId: VersionRuleId) => Future.successful(
          -\/(ConfigurationError.versionNotFound(vrId.versionId))))
      val fe = Await.result(removeRule(VersionRuleId(expectedvId, expectedrId)).run, 1000.milli)
      fe ==== -\/(NotFoundRejection("version not found", Some(Json.toJson(expectedvId))))
    }
    "Remove rule returns fails when rule does not exist" ! {
      def removeRule = RuleService.remove(
        (vrId: VersionRuleId) => Future.successful(
          -\/(ConfigurationError.ruleNotFound(vrId.ruleId))))
      val fe = Await.result(removeRule(VersionRuleId(expectedvId, expectedrId)).run, 1000.milli)
      fe ==== -\/(NotFoundRejection("rule not found", Some(Json.toJson(expectedrId))))
    }
  }
}
