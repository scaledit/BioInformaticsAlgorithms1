package com.ntoggle.kubitschek.integration

import com.ntoggle.albi._
import com.ntoggle.helix.api.rules._
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.domain.{Rule, RuleConditions, RuleId, _}
import org.joda.time.Instant
import org.scalacheck.Prop
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.collection.immutable.Set
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz.{-\/, \/-}
import DomainGenerators._
import com.ntoggle.helix.api.rules.{Rule => HelixRule}

class PublishingServiceSpec extends Specification with ScalaCheck {

  "PublishingService" should {
    val dpId = DemandPartnerId("dpId")
    val spId = SupplyPartnerId("spId")
    val vId = VersionId("vId")
    val pi = PublishedInstant.now
    val emqps = DemandPartnerMaxQps(MaxQps(10000))
    val ce = ConfigurationEndpoint("127.0.0.1", Port(8281))
    val rci = RouterConfigurationId(dpId, spId)
    val rc = RouterConfiguration(rci, ce)
    val rules = List.empty
    val vs = VersionSummary(vId, rci, CreatedInstant(new Instant(123456)), ModifiedInstant(new Instant(123456)), Some(pi), emqps, rules)



    "Publish Configuration" ! {

      def publishConfiguration = PublishingService.publish(
        (vId, pi) => Future.successful(\/-(vs)),
        (rci) => Future.successful(Some(rc)),
        (ce, vs) => Future.successful(())) _

      val fe = Await.result(publishConfiguration(vId, pi), 1000.milli)
      fe ==== \/-(vs)
    }

    "fails when database isn't updated" ! {

      def publishConfiguration = PublishingService.publish(
        (vId, pi) => Future.successful(-\/(ConfigurationError.versionAlreadyPublished(vs.id))),
        (rci) => Future.successful(Some(rc)),
        (ce, vs) => Future.successful(())) _

      val fe = Await.result(publishConfiguration(vId, pi), 1000.milli)
      fe ==== -\/(ConfigurationError.versionAlreadyPublished(vs.id))

    }

    "fails when router config isn't found" ! {

      def publishConfiguration = PublishingService.publish(
        (vId, pi) => Future.successful(\/-(vs)),
        (rci) => Future.successful(None),
        (ce, vs) => Future.successful(())) _

      val fe = Await.result(publishConfiguration(vId, pi), 1000.milli)
      fe ==== -\/(ConfigurationError.routerConfigurationNotFound(rci))

    }

    "converting rules" should {
      "succeed with no conditions" ! {
        val result =
          com.ntoggle.helix.api.rules.Rule(
            com.ntoggle.helix.api.rules.RuleId("123"),
            PValue.Default,
            com.ntoggle.helix.api.rules.RuleConditions.builder())

        val rc = RuleConditions.Empty
        val rule = Rule(RuleId("123"), RuleName("test"),
          RuleCreatedInstant(Instant.now()), Mobile, rc)

        result ==== PublishingService.convertRule(rule, PValue.Default)

      }

      "for presence attribute" should {
        "with allow all as a default condition" should {
          "succeed with empty set" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(AttributeCondition.allowAll)
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(AllowAllDefaultConditionAction, Set.empty, AllowUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with empty set with undefined" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(AttributeCondition.allowAll)
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(AllowAllDefaultConditionAction, Set.empty, ExcludeUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with positive" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(IncludeUnknownAnd(Set.empty))
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(AllowAllDefaultConditionAction, Set(com.ntoggle.kubitschek.domain.LatLong), AllowUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with positive with undefined" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(IncludeUnknownAnd(Set.empty))
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(AllowAllDefaultConditionAction, Set(com.ntoggle.kubitschek.domain.LatLong), ExcludeUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with negative" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(AttributeCondition.allowAllIfKnown)
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(AllowAllDefaultConditionAction, Set(com.ntoggle.kubitschek.domain.NoLatLong), AllowUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with negative with undefined" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(AttributeCondition.allowAllIfKnown)
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(AllowAllDefaultConditionAction, Set(com.ntoggle.kubitschek.domain.NoLatLong), ExcludeUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with both" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(AttributeCondition.excludeAll)
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(AllowAllDefaultConditionAction, Set(com.ntoggle.kubitschek.domain.NoLatLong, com.ntoggle.kubitschek.domain.LatLong), AllowUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with both with undefined" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(AttributeCondition.excludeAll)
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(AllowAllDefaultConditionAction, Set(com.ntoggle.kubitschek.domain.NoLatLong, com.ntoggle.kubitschek.domain.LatLong), ExcludeUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }
        }

        "with exclude all as a default condition" should {
          "succeed with empty set" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(AttributeCondition.excludeAll)
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(ExcludeAllDefaultConditionAction, Set.empty, AllowUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with empty set with undefined" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(AttributeCondition.excludeAll)
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(ExcludeAllDefaultConditionAction, Set.empty, ExcludeUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with positive" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(AttributeCondition.allowAllIfKnown)
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(ExcludeAllDefaultConditionAction, Set(com.ntoggle.kubitschek.domain.LatLong), AllowUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with positive with undefined" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(AttributeCondition.allowAllIfKnown)
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(ExcludeAllDefaultConditionAction, Set(com.ntoggle.kubitschek.domain.LatLong), ExcludeUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with negative" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(IncludeUnknownAnd(Set.empty))
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(ExcludeAllDefaultConditionAction, Set(com.ntoggle.kubitschek.domain.NoLatLong), AllowUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with negative with undefined" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(IncludeUnknownAnd(Set.empty))
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(ExcludeAllDefaultConditionAction, Set(com.ntoggle.kubitschek.domain.NoLatLong), ExcludeUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with both" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(AttributeCondition.allowAll)
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(ExcludeAllDefaultConditionAction, Set(com.ntoggle.kubitschek.domain.NoLatLong, com.ntoggle.kubitschek.domain.LatLong), AllowUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with both with undefined" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  latLong = Some(AttributeCondition.allowAll)
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                latlong = Some(RuleCondition(ExcludeAllDefaultConditionAction, Set(com.ntoggle.kubitschek.domain.NoLatLong, com.ntoggle.kubitschek.domain.LatLong), ExcludeUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }
        }
      }

      "for condition attribute" should {
        "with exclude all as a default condition" should {
          "succeed with allow all and allow undefined and exceptions" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  country = Some(ExcludeOnly(Set(CountryIdAlpha2("AU"))))
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                country = Some(RuleCondition(AllowAllDefaultConditionAction, Set(CountryIdAlpha2("AU")), AllowUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with allow all and exclude undefined and exceptions" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  country = Some(ExcludeUnknownAnd(Set(CountryIdAlpha2("AU"))))
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                country = Some(RuleCondition(AllowAllDefaultConditionAction, Set(CountryIdAlpha2("AU")), ExcludeUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with exclude all and allow undefined and exceptions" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  country = Some(IncludeUnknownAnd(Set(CountryIdAlpha2("AU"))))
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                country = Some(RuleCondition(ExcludeAllDefaultConditionAction, Set(CountryIdAlpha2("AU")), AllowUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }

          "succeed with exclude all and exclude undefined and exceptions" ! {
            val result =
              com.ntoggle.helix.api.rules.Rule(
                com.ntoggle.helix.api.rules.RuleId("123"),
                PValue.Default,
                com.ntoggle.helix.api.rules.RuleConditions.builder(
                  country = Some(IncludeOnly(Set(CountryIdAlpha2("AU"))))
                ))

            val rule = Rule(
              RuleId("123"),
              RuleName("test"),
              RuleCreatedInstant(Instant.now()),
              Mobile,
              RuleConditions.builder(
                country = Some(RuleCondition(ExcludeAllDefaultConditionAction, Set(CountryIdAlpha2("AU")), ExcludeUndefinedConditionAction))
              ))

            result ==== PublishingService.convertRule(rule, PValue.Default)
          }
        }
      }
    }
  }

  "version summary max QPS published" ! Prop.forAllNoShrink(
    genVersionSummary, HelixRule.genRulesWithUniqueIds) { (vs, rules) =>
    PublishingService.toRuleConfiguration(vs, rules.toList).maxQps ====
      vs.maxQps.value
  }
}
