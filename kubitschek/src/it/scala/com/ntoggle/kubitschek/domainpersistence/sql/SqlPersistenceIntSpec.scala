package com.ntoggle.kubitschek
package domainpersistence
package sql

import java.util.UUID
import java.util.concurrent.TimeUnit

import com.ntoggle.albi._
import com.ntoggle.albi.users.UserListId
import com.ntoggle.goldengate.concurrent.ScalaFuture
import com.ntoggle.kubitschek.api.Port
import com.ntoggle.goldengate.slick.{DataSourceConfig, SqlClient}
import com.ntoggle.kubitschek.domain._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.joda.time.Instant
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scalaz._
import scalaz.std.list._
import scalaz.std.option._
import scalaz.std.scalaFuture._
import scalaz.std.string._
import scalaz.syntax.either._
import scalaz.syntax.traverse._

class SqlPersistenceIntSpec
  extends Specification
  with ScalazMatchers
  with LazyLogging {
  sequential

  private val timeout = Duration(60, TimeUnit.SECONDS)

  private val c = ConfigFactory.load("persistence")
  val dbCfg = c.getConfig("db")
  private val dbConfig = DataSourceConfig.fromConfig(dbCfg)

  private val newConditionId =
    () => Future.successful(SqlRuleConditionId(UUID.randomUUID().toString))

  "Demand Partner add and get" ! {
    val TestId = "0001"
    val expected = sampleDemandPartner(DemandPartnerId(s"$TestId"))

    usingClient {
      usingPersistence {
        persistence =>
          for {
            addResult <- persistence.addDemandPartner(expected)
            getResult <- persistence.getDemandPartner(expected.id)
            _ <- persistence.deleteDemandPartner(expected.id)
          } yield
          (addResult must equal(\/.right(expected))) and
            (getResult must equal(Option(expected)))
      }
    }
  }

  "Demand Partner add fails when id already exists" ! {
    val TestId = "0002"
    val dp1 = sampleDemandPartner(DemandPartnerId(s"$TestId"))
    val dp2 = (DemandPartner.nameLens >=> DemandPartnerName.valueLens)
      .mod(_ + " other", dp1)

    usingClient {
      usingPersistence {
        usingDP(dp1) {
          persistence =>
            for {
              add2Result <- persistence.addDemandPartner(dp2)
            } yield add2Result must equal(\/.left(
              ConfigurationError.demandPartnerAlreadyExists(dp1.id)))
        }
      }
    }
  }

  "Demand Partner add fails when name already exists" ! {
    val TestId = "0003"
    val dp1 = sampleDemandPartner(DemandPartnerId(s"$TestId-1"))
    val dp2 = DemandPartner(
      DemandPartnerId(s"$TestId-2"),
      dp1.name)

    usingClient {
      usingPersistence {
        usingDP(dp1) {
          persistence =>
            for {
              add2Result <- persistence.addDemandPartner(dp2)
              _ <- persistence.deleteDemandPartner(dp2.id) // Just in case
            } yield add2Result must equal(\/.left(
              ConfigurationError.demandPartnerNameAlreadyExists(dp2.name)))
        }
      }
    }
  }

  "Demand Partner list succeeds" ! {
    val TestId = "0004"
    val dps =
      for (i <- (10 to 80).toList)
        yield sampleDemandPartner(DemandPartnerId(s"$TestId-$i"))

    usingClient {
      usingPersistence {
        usingDPs(dps) {
          persistence =>
            for {
              first10 <- persistence.listDemandPartners(Offset.Zero, Limit(10))
              first5Take5 <- persistence.listDemandPartners(Offset.Zero, Limit(5))
              skip5Take5 <- persistence.listDemandPartners(Offset(5), Limit(5))
            } yield
            (first10 must equal(first5Take5 ::: skip5Take5)) and
              (first10 must haveSize(10))
        }
      }
    }
  }

  "Demand Partner update succeeds" ! {
    val TestId = "0005"
    val initial = sampleDemandPartner(DemandPartnerId(s"$TestId"))
    val updated = (DemandPartner.nameLens >=> DemandPartnerName.valueLens)
      .mod(_ + " UPDATED", initial)

    usingClient {
      usingPersistence {
        usingDP(initial) {
          persistence =>
            for {
              updateResult <- persistence.setDemandPartner(updated)
              actual <- persistence.getDemandPartner(updated.id)
            } yield
            (updateResult must equal(updated.right)) and
              (actual must equal(Option(updated)))
        }
      }
    }
  }

  "Demand Partner update fails when name already exists" ! {
    val TestId = "0006"
    val dp1 = sampleDemandPartner(DemandPartnerId(s"$TestId-1"))
    val dp2 = sampleDemandPartner(DemandPartnerId(s"$TestId-2"))

    val updated = DemandPartner.nameLens.set(dp2, dp1.name)
    val expected = ConfigurationError.demandPartnerNameAlreadyExists(updated.name)

    usingClient {
      usingPersistence {
        usingDPs(List(dp1, dp2)) {
          persistence =>
            for {
              result <- persistence.setDemandPartner(updated)
            } yield result must equal(expected.left)
        }
      }
    }
  }

  "Demand Partner update fails when demand partner didn't already exist" ! {
    val TestId = "0007"
    val dp = sampleDemandPartner(DemandPartnerId(s"$TestId"))
    val expected = ConfigurationError.demandPartnerNotFound(dp.id)

    usingClient {
      usingPersistence {
        persistence =>
          for {
            result <- persistence.setDemandPartner(dp)
          } yield result must equal(expected.left)
      }
    }
  }

  "Supply Partner add and get" ! {
    val TestId = "0100"
    val expected = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId"))

    usingClient {
      usingPersistence {
        persistence =>
          for {
            addResult <- persistence.addSupplyPartner(expected)
            getResult <- persistence.getSupplyPartner(expected.id)
            _ <- persistence.deleteSupplyPartner(expected.id)
          } yield
          (addResult must equal(\/.right(expected))) and
            (getResult must equal(Option(expected)))
      }
    }
  }

  "Supply Partner add fails when id already exists" ! {
    val TestId = "0101"
    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId"))

    val expected = ConfigurationError.supplyPartnerAlreadyExists(sp.id)

    usingClient {
      usingPersistence {
        usingSP(sp) {
          persistence =>
            for {
              addResult <- persistence.addSupplyPartner(sp)
            } yield addResult must equal(\/.left(expected))
        }
      }
    }
  }

  "Supply Partner add fails when name already exists" ! {
    val TestId = "0102"
    val sp1 = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-1"))

    val sp2 = SupplyPartner(
      SupplyPartnerId(s"$TestId-2"),
      sp1.name)

    val expected = ConfigurationError.supplyPartnerNameAlreadyExists(sp2.name)

    usingClient {
      usingPersistence {
        usingSP(sp1) {
          persistence =>
            for {
              addResult <- persistence.addSupplyPartner(sp2)
              _ <- persistence.deleteSupplyPartner(sp2.id) // Just in case
            } yield addResult must equal(\/.left(expected))
        }
      }
    }
  }

  "Supply Partner list succeeds" ! {
    val TestId = "0103"
    val sps =
      for (i <- (10 to 80).toList)
        yield sampleSupplyPartner(
          SupplyPartnerId(s"$TestId-$i"))

    usingClient {
      usingPersistence {
        usingSPs(sps) {
          persistence =>
            for {
              first10 <- persistence.listSupplyPartners(Offset.Zero, Limit(10))
              first5Take5 <- persistence.listSupplyPartners(Offset.Zero, Limit(5))
              skip5Take5 <- persistence.listSupplyPartners(Offset(5), Limit(5))
            } yield
            (first10 must equal(first5Take5 ::: skip5Take5)) and
              (first10 must haveSize(10))
        }
      }
    }
  }

  "Supply Partner update succeeds when setting name" ! {
    val TestId = "0104"
    val initial = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId"))
    val expected = (SupplyPartner.nameLens >=> SupplyPartnerName.valueLens)
      .mod(_ + " UPDATED", initial)

    usingClient {
      usingPersistence {
        usingSP(initial) {
          persistence =>
            for {
              updateResult <- persistence.setSupplyPartner(expected)
              getResult <- persistence.getSupplyPartner(expected.id)
            } yield
            (updateResult must equal(expected.right)) and
              (getResult must equal(Option(expected)))

        }
      }
    }
  }

  "Supply Partner update fails when name already exists" ! {
    val TestId = "0105"
    val sp1 = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-1"))

    val sp2 = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-2"))

    val updated = SupplyPartner.nameLens.set(sp2, sp1.name)
    val expected = ConfigurationError.supplyPartnerNameAlreadyExists(updated.name)

    usingClient {
      usingPersistence {
        usingSP(sp1) {
          usingSP(sp2) {
            persistence =>
              for {
                updateResult <- persistence.setSupplyPartner(updated)
              } yield updateResult must equal(\/.left(expected))
          }
        }
      }
    }
  }


  "RouterConfiguration insert and get with no targets" ! {
    val TestId = "0200"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val expectedCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val expectedV = Version(
      VersionId(s"$TestId-v"),
      expectedCfg.id,
      CreatedInstant(new Instant(12345)),
      ModifiedInstant(new Instant(12345)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    usingClient {
      usingPersistence {
        usingDP(dp) {
          usingSP(sp) {
            p =>
              for {
                addResult <- p.addRouterConfiguration(
                  expectedCfg,
                  expectedV.id,
                  expectedV.created,
                  expectedV.maxQps)

                versionResult <- p.getVersion(expectedV.id)
                cfgResult <- p.getRouterConfiguration(expectedCfg.id)
                _ <- p.deleteVersion(expectedV.id)
                _ <- p.deleteRouterConfiguration(expectedCfg.id)
              } yield
              (addResult.leftMap(_.toString) must equal(expectedCfg.right[String])) and
                (versionResult must equal(Option(expectedV))) and
                (cfgResult must equal(Option(expectedCfg)))
          }
        }
      }
    }
  }

  "RouterConfiguration insert and get with with targets" ! {
    val TestId = "0201"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val expectedCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("0.0.0.0", Port(23455)))

    val expectedV = Version(
      VersionId(s"$TestId-v"),
      expectedCfg.id,
      CreatedInstant(new Instant(12345)),
      ModifiedInstant(new Instant(12345)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    usingClient {
      usingPersistence {
        usingDP(dp) {
          usingSP(sp) {
            p =>
              for {
                addResult <- p.addRouterConfiguration(
                  expectedCfg,
                  expectedV.id,
                  expectedV.created,
                  expectedV.maxQps)

                versionResult <- p.getVersion(expectedV.id)
                cfgResult <- p.getRouterConfiguration(expectedCfg.id)
                _ <- p.deleteVersion(expectedV.id)
                _ <- p.deleteRouterConfiguration(expectedCfg.id)
              } yield
              (addResult.leftMap(_.toString) must equal(expectedCfg.right[String])) and
                (versionResult must equal(Option(expectedV))) and
                (cfgResult must equal(Option(expectedCfg)))
          }
        }
      }
    }
  }

  "RouterConfiguration insert and fails when DP does not exist" ! {
    val TestId = "0202"
    val dpId = DemandPartnerId(s"$TestId-dp")

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val cfg = RouterConfiguration(
      RouterConfigurationId(dpId, sp.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val expectedV = Version(
      VersionId(s"$TestId-v"),
      cfg.id,
      CreatedInstant(new Instant(12345)),
      ModifiedInstant(new Instant(12345)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val expectedError = ConfigurationError.demandPartnerNotFound(dpId)

    usingClient {
      usingPersistence {
        // do not insert DP here
        usingSP(sp) {
          p =>
            for {
              addResult <- p.addRouterConfiguration(
                cfg,
                expectedV.id,
                expectedV.created,
                expectedV.maxQps)
            } yield
            addResult must equal(expectedError.left)
        }
      }
    }
  }
  "RouterConfiguration insert fails when SP does not exist" ! {
    val TestId = "0203"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val spId = SupplyPartnerId(s"$TestId-sp")

    val cfg = RouterConfiguration(
      RouterConfigurationId(dp.id, spId), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val expectedV = Version(
      VersionId(s"$TestId-v"),
      cfg.id,
      CreatedInstant(new Instant(12345)),
      ModifiedInstant(new Instant(12345)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val expectedError = ConfigurationError.supplyPartnerNotFound(spId)

    usingClient {
      usingPersistence {
        usingDP(dp) {
          // do not insert SP here
          p =>
            for {
              addResult <- p.addRouterConfiguration(
                cfg,
                expectedV.id,
                expectedV.created,
                expectedV.maxQps)
            } yield
            addResult must equal(expectedError.left)
        }
      }
    }
  }

  "RouterConfiguration insert and fails when configuration already exists" ! {
    val TestId = "0204"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val cfg1 = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("0.0.0.0", Port(23455)))

    val v = Version(
      VersionId(s"$TestId-v"),
      cfg1.id,
      CreatedInstant(new Instant(12345)),
      ModifiedInstant(new Instant(12345)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val cfg2 = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("0.0.0.0", Port(23455)))

    usingClient {
      usingPersistence {
        usingDP(dp) {
          usingSP(sp) {
            p =>
              for {
                _ <- p.addRouterConfiguration(
                  cfg1,
                  v.id,
                  v.created,
                  v.maxQps)

                _ <- p.getVersion(v.id)

                addResult <- p.addRouterConfiguration(
                  cfg2, // add new cfg
                  v.id,
                  v.created,
                  v.maxQps)

                _ <- p.deleteVersion(v.id)
                _ <- p.deleteRouterConfiguration(cfg1.id)
              } yield addResult must equal(
                ConfigurationError.routerConfigurationAlreadyExists(cfg1.id).left)
          }
        }
      }
    }
  }

  "RouterConfiguration list" ! {
    val TestId = "0206"
    val dp1 = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp1"))

    val dpOther = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dpOther"))

    val sp1 = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp1"))
    val sp2 = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp2"))

    val expectedCfg1 = RouterConfiguration(
      RouterConfigurationId(dp1.id, sp1.id),
      ConfigurationEndpoint("0.0.0.0", Port(23455)))

    val expectedCfg2 = RouterConfiguration(
      RouterConfigurationId(dp1.id, sp2.id), ConfigurationEndpoint("ntoggle.com", Port(4000))) // Make sure this cfg has no targets

    val otherCfg = RouterConfiguration(
      RouterConfigurationId(dpOther.id, sp2.id), // Use other DP
      ConfigurationEndpoint("127.0.0.1", Port(12345)))

    val v1 = VersionId(s"$TestId-v1")
    val v2 = VersionId(s"$TestId-v2")
    val vOther = VersionId(s"$TestId-vOther")

    val instant = CreatedInstant(new Instant(12345))
    val maxQps = DemandPartnerMaxQps(MaxQps(345366))

    val expected = List(expectedCfg1, expectedCfg2)
      .sortBy(_.id)(implicitly[Order[RouterConfigurationId]].toScalaOrdering)

    usingClient {
      usingPersistence {
        usingDPs(List(dp1, dpOther)) {
          usingSPs(List(sp1, sp2)) {
            usingRC(expectedCfg1, v1, instant, maxQps) {
              usingRC(expectedCfg2, v2, instant, maxQps) {
                usingRC(otherCfg, vOther, instant, maxQps) {
                  p =>
                    for {
                      actual <- p.getRouterConfigurations(dp1.id)
                    } yield actual must equal(Option(expected))
                }
              }
            }
          }
        }
      }
    }
  }

  "RouterConfiguration list with non-existent Demand Partner returns None" ! {
    val TestId = "0207"
    val dp1 = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp1"))

    val dpOther = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dpOther"))

    val sp1 = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp1"))

    val expectedCfg1 = RouterConfiguration(
      RouterConfigurationId(dp1.id, sp1.id),
      ConfigurationEndpoint("0.0.0.0", Port(23455)))

    val v1 = VersionId(s"$TestId-v1")

    val instant = CreatedInstant(new Instant(12345))
    val maxQps = DemandPartnerMaxQps(MaxQps(345366))

    usingClient {
      usingPersistence {
        usingDP(dp1) {
          usingSP(sp1) {
            usingRC(expectedCfg1, v1, instant, maxQps) {
              p =>
                for {
                  actual <- p.getRouterConfigurations(dpOther.id)
                } yield actual must equal(Option.empty)
            }
          }
        }
      }
    }
  }

  "Versions add and get" ! {
    val TestId = "0300"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val maxQps = DemandPartnerMaxQps(MaxQps(345366))

    val v1 = Version(
      VersionId(s"$TestId-v1"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      maxQps)
    val expectedVersion = Version(
      VersionId(s"$TestId-v2"),
      dpCfg.id,
      CreatedInstant(new Instant(55555)),
      ModifiedInstant(new Instant(55555)),
      None,
      maxQps)

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v1.id, v1.created, v1.maxQps) {
              p =>
                for {
                  addResult <- p.addVersion(
                    expectedVersion.id,
                    expectedVersion.routerConfigurationId,
                    expectedVersion.created,
                    expectedVersion.maxQps)
                  actual <- p.getVersion(expectedVersion.id)
                  _ <- p.deleteVersion(expectedVersion.id)
                } yield actual must equal(Option(expectedVersion))
            }
          }
        }
      }
    }
  }

  "Versions add fails when version already exists" ! {
    val TestId = "0301"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val maxQps = DemandPartnerMaxQps(MaxQps(345366))

    val v1 = Version(
      VersionId(s"$TestId-v1"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      maxQps)

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v1.id, v1.created, v1.maxQps) {
              p =>
                for {
                  addResult <- p.addVersion(
                    v1.id,
                    v1.routerConfigurationId,
                    v1.created,
                    v1.maxQps)
                } yield addResult must equal(
                  ConfigurationError.versionAlreadyExists(v1.id).left)
            }
          }
        }
      }
    }
  }

  "Versions add fails when router configuration doesn't exist" ! {
    val TestId = "0302"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfgId = RouterConfigurationId(dp.id, sp.id)

    val maxQps = DemandPartnerMaxQps(MaxQps(345366))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfgId,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      maxQps)

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            p =>
              for {
                addResult <- p.addVersion(
                  v.id,
                  v.routerConfigurationId,
                  v.created,
                  v.maxQps)
              } yield addResult must equal(
                ConfigurationError.routerConfigurationNotFound(dpCfgId).left)
          }
        }
      }
    }
  }

  "Versions list" ! {
    val TestId = "0303"
    val dp1 = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp1"))

    val dp2 = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp2"))

    val sp1 = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp1"))
    val sp2 = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp2"))

    val dp1cfg1 = RouterConfiguration(
      RouterConfigurationId(dp1.id, sp1.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val dp1cfg2 = RouterConfiguration(
      RouterConfigurationId(dp1.id, sp2.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val dp2cfg1 = RouterConfiguration(
      RouterConfigurationId(dp2.id, sp2.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val maxQps = DemandPartnerMaxQps(MaxQps(345366))

    val v1 = Version(
      VersionId(s"$TestId-v1"),
      dp1cfg1.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      maxQps)
    val v2 = Version(
      VersionId(s"$TestId-v2"),
      dp1cfg2.id,
      CreatedInstant(new Instant(22222)),
      ModifiedInstant(new Instant(22222)),
      None,
      maxQps)
    val v3 = Version(
      VersionId(s"$TestId-v3"),
      dp2cfg1.id,
      CreatedInstant(new Instant(22233)),
      ModifiedInstant(new Instant(22233)),
      None,
      maxQps)

    // results are sorted in DESC time order.
    val expected = List(v1, v3, v2)

    // Remove versions from other tests
    def thisTestOnly(v: Version): Boolean =
      VersionId.orderz.equal(v.id, v1.id) ||
        VersionId.orderz.equal(v.id, v2.id) ||
        VersionId.orderz.equal(v.id, v3.id)

    usingClient {
      usingPersistence {
        usingDPs(List(dp1, dp2)) {
          usingSPs(List(sp1, sp2)) {
            usingRC(dp1cfg1, v1.id, v1.created, v1.maxQps) {
              usingRC(dp1cfg2, v2.id, v2.created, v2.maxQps) {
                usingRC(dp2cfg1, v3.id, v3.created, v3.maxQps) {
                  p =>
                    for {
                      actual <- p.listVersions(None, None, Offset(0), Limit(100))
                    } yield actual.filter(thisTestOnly) must equal(expected)
                }
              }
            }
          }
        }
      }
    }
  }

  "Versions list by dpId" ! {
    val TestId = "0304"
    val dp1 = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp1"))

    val dp2 = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp2"))

    val sp1 = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp1"))
    val sp2 = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp2"))

    val dp1cfg1 = RouterConfiguration(
      RouterConfigurationId(dp1.id, sp1.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val dp1cfg2 = RouterConfiguration(
      RouterConfigurationId(dp1.id, sp2.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val dp2cfg1 = RouterConfiguration(
      RouterConfigurationId(dp2.id, sp2.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val maxQps = DemandPartnerMaxQps(MaxQps(345366))

    val v1 = Version(
      VersionId(s"$TestId-v1"),
      dp1cfg1.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      maxQps)
    val v2 = Version(
      VersionId(s"$TestId-v2"),
      dp1cfg2.id,
      CreatedInstant(new Instant(22222)),
      ModifiedInstant(new Instant(22222)),
      None,
      maxQps)
    val v3 = Version(
      VersionId(s"$TestId-v3"),
      dp2cfg1.id,
      CreatedInstant(new Instant(11111)),
      ModifiedInstant(new Instant(11111)),
      None,
      maxQps)

    // results are sorted in DESC time order
    val expectedDp1Versions = List(v1, v2)
    val expectedDp2Versions = List(v3)

    usingClient {
      usingPersistence {
        usingDPs(List(dp1, dp2)) {
          usingSPs(List(sp1, sp2)) {
            usingRC(dp1cfg1, v1.id, v1.created, v1.maxQps) {
              usingRC(dp1cfg2, v2.id, v2.created, v2.maxQps) {
                usingRC(dp2cfg1, v3.id, v3.created, v3.maxQps) {
                  p =>
                    for {
                      actualDp1Versions <- p.listVersions(Some(dp1.id), None, Offset(0), Limit(10))
                      actualDp2Versions <- p.listVersions(Some(dp2.id), None, Offset(0), Limit(10))
                    } yield
                    (actualDp1Versions must equal(expectedDp1Versions)) and
                      (actualDp2Versions must equal(expectedDp2Versions))
                }
              }
            }
          }
        }
      }
    }
  }

  "Versions list by spId" ! {
    val TestId = "0305"
    val dp1 = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp1"))

    val dp2 = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp2"))

    val sp1 = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp1"))
    val sp2 = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp2"))

    val sp1cfg1 = RouterConfiguration(
      RouterConfigurationId(dp1.id, sp1.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val sp2cfg1 = RouterConfiguration(
      RouterConfigurationId(dp1.id, sp2.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val sp2cfg2 = RouterConfiguration(
      RouterConfigurationId(dp2.id, sp2.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val maxQps = DemandPartnerMaxQps(MaxQps(345366))

    val v1 = Version(
      VersionId(s"$TestId-v1"),
      sp1cfg1.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      maxQps)
    val v2 = Version(
      VersionId(s"$TestId-v2"),
      sp2cfg1.id,
      CreatedInstant(new Instant(22222)),
      ModifiedInstant(new Instant(22222)),
      None,
      maxQps)
    val v3 = Version(
      VersionId(s"$TestId-v3"),
      sp2cfg2.id,
      CreatedInstant(new Instant(11111)),
      ModifiedInstant(new Instant(11111)),
      None,
      maxQps)

    // results are sorted in DESC time order.
    val expectedSp1Versions = List(v1)
    val expectedSp2Versions = List(v2, v3)

    usingClient {
      usingPersistence {
        usingDPs(List(dp1, dp2)) {
          usingSPs(List(sp1, sp2)) {
            usingRC(sp1cfg1, v1.id, v1.created, v1.maxQps) {
              usingRC(sp2cfg1, v2.id, v2.created, v2.maxQps) {
                usingRC(sp2cfg2, v3.id, v3.created, v3.maxQps) {
                  p =>
                    for {
                      actualSp1Versions <- p.listVersions(None, Some(sp1.id), Offset(0), Limit(10))
                      actualSp2Versions <- p.listVersions(None, Some(sp2.id), Offset(0), Limit(10))
                    } yield
                    (actualSp1Versions must equal(expectedSp1Versions)) and
                      (actualSp2Versions must equal(expectedSp2Versions))
                }
              }
            }
          }
        }
      }
    }
  }

  // 0306 0 list by spType is no longer supported.

  "Publish Version" ! {
    val TestId = "0307"

    val publishInstant = PublishedInstant(new Instant(307))
    val publishInstant2 = PublishedInstant(new Instant(3071))

    val dp1 = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp1"))

    val sp1 = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp1"))
    val type1cfg1 = RouterConfiguration(
      RouterConfigurationId(dp1.id, sp1.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val maxQps = DemandPartnerMaxQps(MaxQps(345366))

    val v1 = Version(
      VersionId(s"$TestId-v1"),
      type1cfg1.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      maxQps)

    val v1Expected = Version(
      VersionId(s"$TestId-v1"),
      type1cfg1.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      Some(publishInstant),
      maxQps)

    usingClient {
      usingPersistence {
        usingDPs(List(dp1)) {
          usingSPs(List(sp1)) {
            usingRC(type1cfg1, v1.id, v1.created, v1.maxQps) {
              p =>
                for {
                  result <- p.publishVersion(v1.id, publishInstant)
                  actual <- p.getVersion(v1.id)
                  resultFail <- p.publishVersion(v1.id, publishInstant2)
                } yield
                (actual must equal(Option(v1Expected))) and
                  (resultFail must equal(ConfigurationError.versionAlreadyPublished(v1.id).left))
            }
          }
        }
      }
    }
  }

  "Rule add empty rule and Get" ! {
    val TestId = "0400"
    val expected = Rule(
      RuleId(s"$TestId-r"),
      RuleName(s"$TestId-name"),
      RuleCreatedInstant(new Instant(1111111)),
      Mobile,
      RuleConditions.Empty)

    usingClient {
      usingPersistence {
        p => for {
          addResult <- p.addRule(expected)
          actual <- p.getRule(expected.id)
          _ <- p.deleteRule(expected.id)
        } yield
          (addResult must equal(expected.right)) and
            (actual must equal(Option(expected)))
      }
    }
  }

  "Rule add duplicate empty rule results in error" ! {
    val TestId = "0401"
    val r1 = Rule(
      RuleId(s"$TestId-r1"),
      RuleName(s"$TestId-name1"),
      RuleCreatedInstant(new Instant(1111111)),
      Mobile,
      RuleConditions.Empty)
    val r2 = Rule(
      r1.id,
      RuleName(s"$TestId-name2"),
      RuleCreatedInstant(new Instant(2222222)),
      Mobile,
      RuleConditions.Empty)

    val expected = ConfigurationError.ruleAlreadyExists(r1.id)

    usingClient {
      usingPersistence {
        p => for {
          _ <- p.addRule(r1)
          addResult <- p.addRule(r2)
          _ <- p.deleteRule(r1.id)
        } yield addResult must equal(expected.left)
      }
    }
  }

  "Rule add rule with conditions and get" ! {
    val TestId = "0402"
    val expected = sampleRule(TestId, RuleName("r"))
    usingClient {
      usingPersistence {
        p => for {
          addResult <- p.addRule(expected)
          actual <- p.getRule(expected.id)
          _ <- p.deleteRule(expected.id)
        } yield
          (addResult must equal(expected.right)) and
            (actual must equal(Option(expected)))
      }
    }
  }

  "Versions/Rules add rule to version and get version summary" ! {
    val TestId = "0500"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r = sampleRule(TestId, RuleName("r"))
    val ruleQps = DesiredToggledQps(10000)

    val versionSummary = VersionRuleSummary(
      VersionRuleId(v.id, r.id),
      r.trafficType,
      ruleQps,
      r.created,
      r.name)
    val expected = VersionSummary.fromVersionAndSummaries(
      v,
      List(versionSummary))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r)) {
                p =>
                  for {
                    addResult <- p.addRuleToVersion(v.id, r.id, ruleQps)
                    actual <- p.getVersionSummary(v.id)
                    _ <- p.removeRuleFromVersion(versionSummary.id)
                  } yield
                  (addResult must equal(r.id.right)) and
                    (actual must equal(Option(expected)))
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules add rule to version and get version summaries by DPID and SPID" ! {
    val TestId = "0500a"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r = sampleRule(TestId, RuleName("r"))
    val ruleQps = DesiredToggledQps(10000)

    val versionSummary = VersionRuleSummary(
      VersionRuleId(v.id, r.id),
      r.trafficType,
      ruleQps,
      r.created,
      r.name)
    val expected = VersionSummary.fromVersionAndSummaries(
      v,
      List(versionSummary))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r)) {
                p =>
                  for {
                    addResult <- p.addRuleToVersion(v.id, r.id, ruleQps)
                    actual <- p.listVersionSummaries(Some(dp.id),Some(sp.id),Offset(0),Limit(20))
                    _ <- p.removeRuleFromVersion(versionSummary.id)
                  } yield
                    (addResult must equal(r.id.right)) and
                      (actual must equal(List(expected)))
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules add rule to version and get version summaries by DPID" ! {
    val TestId = "0500b"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r = sampleRule(TestId, RuleName("r"))
    val ruleQps = DesiredToggledQps(10000)

    val versionSummary = VersionRuleSummary(
      VersionRuleId(v.id, r.id),
      r.trafficType,
      ruleQps,
      r.created,
      r.name)
    val expected = VersionSummary.fromVersionAndSummaries(
      v,
      List(versionSummary))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r)) {
                p =>
                  for {
                    addResult <- p.addRuleToVersion(v.id, r.id, ruleQps)
                    actual <- p.listVersionSummaries(Some(dp.id),None,Offset(0),Limit(20))
                    _ <- p.removeRuleFromVersion(versionSummary.id)
                  } yield
                    (addResult must equal(r.id.right)) and
                      (actual must equal(List(expected)))
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules add rule to version and get version summaries by SPID" ! {
    val TestId = "0500c"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r = sampleRule(TestId, RuleName("r"))
    val ruleQps = DesiredToggledQps(10000)

    val versionSummary = VersionRuleSummary(
      VersionRuleId(v.id, r.id),
      r.trafficType,
      ruleQps,
      r.created,
      r.name)
    val expected = VersionSummary.fromVersionAndSummaries(
      v,
      List(versionSummary))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r)) {
                p =>
                  for {
                    addResult <- p.addRuleToVersion(v.id, r.id, ruleQps)
                    actual <- p.listVersionSummaries(None,Some(sp.id),Offset(0),Limit(20))
                    _ <- p.removeRuleFromVersion(versionSummary.id)
                  } yield
                    (addResult must equal(r.id.right)) and
                      (actual must equal(List(expected)))
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules add rule to version fails when rule already associated to version" ! {
    val TestId = "0501"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r = sampleRule(TestId, RuleName("r"))
    val ruleQps = DesiredToggledQps(10000)

    val versionSummary = VersionRuleSummary(
      VersionRuleId(v.id, r.id),
      r.trafficType,
      ruleQps,
      r.created,
      r.name)
    val expected = ConfigurationError.ruleAlreadyExists(r.id)

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r)) {
                p =>
                  for {
                    _ <- p.addRuleToVersion(v.id, r.id, ruleQps)
                    addResult <- p.addRuleToVersion(v.id, r.id, ruleQps)
                    _ <- p.removeRuleFromVersion(versionSummary.id)
                  } yield addResult must equal(expected.left)
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules add rule to version fails when version not found" ! {
    val TestId = "0502"

    val vId = VersionId(s"$TestId-v")
    val r = sampleRule(TestId, RuleName("r"))
    val ruleQps = DesiredToggledQps(10000)

    val expected = ConfigurationError.versionNotFound(vId)

    usingClient {
      usingPersistence {
        usingRules(List(r)) {
          p =>
            for {
              addResult <- p.addRuleToVersion(vId, r.id, ruleQps)
            } yield addResult must equal(expected.left[RuleId])
        }
      }
    }
  }

  "Versions/Rules add rule to version fails when rule not found" ! {
    val TestId = "0503"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val rId = RuleId(s"$TestId-r")
    val ruleQps = DesiredToggledQps(10000)

    val expected = ConfigurationError.ruleNotFound(rId)

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              p =>
                for {
                  addResult <- p.addRuleToVersion(v.id, rId, ruleQps)
                } yield addResult must equal(expected.left)
            }
          }
        }
      }
    }
  }

  "Versions/Rules replace rule in version" ! {
    val TestId = "0504"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v1 = Version(
      VersionId(s"$TestId-v1"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val v2 = Version(
      VersionId(s"$TestId-v2"),
      dpCfg.id,
      CreatedInstant(new Instant(44444)),
      ModifiedInstant(new Instant(44444)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    // Need to set created instant for deterministic ordering
    val r1 = Rule.createdValueLens.mod(
      _.plus(1),
      sampleRule(TestId, RuleName("r1")))

    val r2 = Rule.createdValueLens.mod(
      _.plus(2),
      sampleRule(TestId, RuleName("r2")))

    val r3 = Rule.createdValueLens.mod(
      _.plus(3),
      sampleRule(TestId, RuleName("r3")))

    val v1r1Summary = VersionRuleSummary(
      VersionRuleId(v1.id, r1.id),
      r1.trafficType,
      DesiredToggledQps(10000),
      r1.created,
      r1.name)

    val v1r2Summary = VersionRuleSummary(
      VersionRuleId(v1.id, r2.id),
      r2.trafficType,
      DesiredToggledQps(20000),
      r2.created,
      r2.name)

    val v1r3Summary = VersionRuleSummary(
      VersionRuleId(v1.id, r3.id),
      r3.trafficType,
      v1r2Summary.desiredQps,
      r3.created,
      r3.name)

    val v2r3Summary = VersionRuleSummary(
      VersionRuleId(v2.id, r3.id),
      r3.trafficType,
      DesiredToggledQps(35000),
      r3.created,
      r3.name)

    val v1Expected =
      VersionSummary.fromVersionAndSummaries(v1, List(v1r3Summary, v1r1Summary))

    val v2Expected =
      VersionSummary.fromVersionAndSummaries(v2, List(v2r3Summary))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v1.id, v1.created, v1.maxQps) {
              usingV(v2.id, v2.routerConfigurationId, v2.created, v2.maxQps) {
                usingRules(List(r1, r2, r3)) {
                  p =>
                    for {
                      _ <- p.addRuleToVersion(
                        v1r1Summary.id.versionId,
                        v1r1Summary.id.ruleId,
                        v1r1Summary.desiredQps)

                      _ <- p.addRuleToVersion(
                        v1r2Summary.id.versionId,
                        v1r2Summary.id.ruleId,
                        v1r2Summary.desiredQps)

                      _ <- p.addRuleToVersion(
                        v2r3Summary.id.versionId,
                        v2r3Summary.id.ruleId,
                        v2r3Summary.desiredQps)

                      replaceResult <- p.replaceRuleInVersion(
                        v1r2Summary.id,
                        v1r3Summary.id.ruleId)

                      v1Actual <- p.getVersionSummary(v1.id)
                      v2Actual <- p.getVersionSummary(v2.id)

                      // Remove all just in case
                      _ <- p.removeRuleFromVersion(v1r1Summary.id)
                      _ <- p.removeRuleFromVersion(v1r2Summary.id)
                      _ <- p.removeRuleFromVersion(v1r3Summary.id)
                      _ <- p.removeRuleFromVersion(v2r3Summary.id)
                    } yield
                    (replaceResult must equal(v1r3Summary.id.ruleId.right)) and
                      (v1Actual must equal(Option(v1Expected))) and
                      (v2Actual must equal(Option(v2Expected)))
                }
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules replace rule fails when rule already associated to version" ! {
    val TestId = "0505"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r1 = sampleRule(TestId, RuleName("r1"))
    val r2 = sampleRule(TestId, RuleName("r2"))
    val expected = ConfigurationError.ruleAlreadyExists(r1.id)

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r1, r2)) {
                p =>
                  for {
                    _ <- p.addRuleToVersion(
                      v.id,
                      r1.id,
                      DesiredToggledQps(12345))

                    _ <- p.addRuleToVersion(
                      v.id,
                      r2.id,
                      DesiredToggledQps(23445))

                    replaceResult <- p.replaceRuleInVersion(
                      VersionRuleId(v.id, r2.id),
                      r1.id)

                    // Remove all just in case
                    _ <- p.removeRuleFromVersion(VersionRuleId(v.id, r1.id))
                    _ <- p.removeRuleFromVersion(VersionRuleId(v.id, r2.id))
                  } yield replaceResult must equal(expected.left)
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules replace rule fails when replaced rule not associated to version" ! {
    val TestId = "0506"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r1 = sampleRule(TestId, RuleName("r1"))
    val r2 = sampleRule(TestId, RuleName("r2"))
    val expected =
      ConfigurationError.ruleNotInVersion(VersionRuleId(v.id, r1.id))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r1, r2)) {
                p =>
                  for {
                    replaceResult <- p.replaceRuleInVersion(
                      VersionRuleId(v.id, r1.id),
                      r2.id)
                  } yield replaceResult must equal(expected.left)
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules replace rule fails when replaced rule not associated to version" ! {
    val TestId = "0506"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r1 = sampleRule(TestId, RuleName("r1"))
    val r2 = sampleRule(TestId, RuleName("r2"))
    val expected =
      ConfigurationError.ruleNotInVersion(VersionRuleId(v.id, r1.id))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r1, r2)) {
                p =>
                  for {
                    replaceResult <- p.replaceRuleInVersion(
                      VersionRuleId(v.id, r1.id),
                      r2.id)
                  } yield replaceResult must equal(expected.left)
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules replace rule fails when version doesn't exist" ! {
    val TestId = "0507"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v1 = Version(
      VersionId(s"$TestId-v1"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val v2 = Version(
      VersionId(s"$TestId-v2"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r1 = sampleRule(TestId, RuleName("r1"))
    val r2 = sampleRule(TestId, RuleName("r2"))
    val expected =
      ConfigurationError.ruleNotInVersion(VersionRuleId(v2.id, r1.id))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            // create a version for dpCfg just in case
            usingRC(dpCfg, v1.id, v1.created, v1.maxQps) {
              usingRules(List(r1, r2)) {
                p =>
                  for {
                    replaceResult <- p.replaceRuleInVersion(
                      VersionRuleId(v2.id, r1.id),
                      r2.id)
                  } yield replaceResult must equal(expected.left)
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules replace rule fails when rule doesn't exist" ! {
    val TestId = "0508"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v1"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r1 = sampleRule(TestId, RuleName("r1"))
    val r2 = sampleRule(TestId, RuleName("r2"))
    val expected =
      ConfigurationError.ruleNotFound(r1.id)

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            // create a version for dpCfg just in case
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r1)) {
                // r2 not added
                p =>
                  for {
                    _ <- p.addRuleToVersion(
                      v.id,
                      r1.id,
                      DesiredToggledQps(12345))

                    replaceResult <- p.replaceRuleInVersion(
                      VersionRuleId(v.id, r1.id),
                      r2.id)

                    _ <- p.removeRuleFromVersion(VersionRuleId(v.id, r1.id))
                  } yield replaceResult must equal(expected.left)
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules save rule to version" ! {
    val TestId = "0509"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r = sampleRule(TestId, RuleName("r"))
    val ruleQps = DesiredToggledQps(10000)

    val vrId = VersionRuleId(v.id, r.id)

    val vrs = VersionRuleSummary(
      vrId,
      r.trafficType,
      ruleQps,
      r.created,
      r.name)

    val expected = VersionSummary.fromVersionAndSummaries(
      v,
      List(vrs))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRulesToClean(List(r)) {
                p =>
                  for {
                    addResult <- p.saveRuleToVersion(v.id, r, ruleQps)
                    actual <- p.getVersionSummary(v.id)
                    _ <- p.removeRuleFromVersion(vrId)
                  } yield
                  (addResult must equal(r.right)) and
                    (actual must equal(Option(expected)))
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules save rule to version, get Rule by Id" ! {
    val TestId = "0509a"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r = sampleRule(TestId, RuleName("r"))
    val ruleQps = DesiredToggledQps(10000)

    val vrId = VersionRuleId(v.id, r.id)

    val vrs = VersionRuleSummary(
      vrId,
      r.trafficType,
      ruleQps,
      r.created,
      r.name)

    val expected = VersionSummary.fromVersionAndSummaries(
      v,
      List(vrs))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRulesToClean(List(r)) {
                p =>
                  for {
                    addResult <- p.saveRuleToVersion(v.id, r, ruleQps)
                    actual <- p.getVersionSummary(v.id)
                    rule <- p.getRule(r.id)
                    _ <- p.removeRuleFromVersion(vrId)
                  } yield
                  (addResult must equal(r.right)) and
                    (actual must equal(Option(expected))) and
                    (rule must equal(Option(r)))
              }
            }
          }
        }
      }
    }
  }
  "Versions/Rules save rule to version, get Versions For Rule" ! {
    val TestId = "0509b"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r = sampleRule(TestId, RuleName("r"))
    val ruleQps = DesiredToggledQps(10000)

    val vrId = VersionRuleId(v.id, r.id)

    val vrs = VersionRuleSummary(
      vrId,
      r.trafficType,
      ruleQps,
      r.created,
      r.name)

    val expected = VersionSummary.fromVersionAndSummaries(
      v,
      List(vrs))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRulesToClean(List(r)) {
                p =>
                  for {
                    addResult <- p.saveRuleToVersion(v.id, r, ruleQps)
                    actual <- p.getVersionSummary(v.id)
                    vv <- p.listVersionsForRule(r.id, dp.id, List(sp.id), Offset(0), Limit(1))
                    _ <- p.removeRuleFromVersion(vrId)
                  } yield
                  (addResult must equal(r.right)) and
                    (actual must equal(Option(expected))) and
                    (vv must equal(List(v)))
              }
            }
          }
        }
      }
    }
  }
  "Versions/Rules save rule to version fails for published Version" ! {
    val TestId = "0510"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r = sampleRule(TestId, RuleName("r"))
    val ruleQps = DesiredToggledQps(10000)
    val expected =
      ConfigurationError.versionAlreadyPublished(v.id).left

    val publishInstant = PublishedInstant(new Instant(510))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              p =>
                for {
                  vs <- p.publishVersion(v.id, publishInstant)
                  addResult <- p.saveRuleToVersion(v.id, r, ruleQps)
                } yield addResult must equal(expected)
            }
          }
        }
      }
    }
  }
  "Versions/Rules save rule to version fails for rule exists" ! {
    val TestId = "0511"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r = sampleRule(TestId, RuleName("r"))
    val ruleQps = DesiredToggledQps(10000)
    val expected =
      ConfigurationError.ruleAlreadyExists(r.id).left

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r)) {
                p =>
                  for {
                    addResult <- p.saveRuleToVersion(v.id, r, ruleQps)
                  } yield addResult must equal(expected)
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules updateRuleInVersion" ! {
    val TestId = "0512"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r1 = sampleRule(TestId, RuleName("r1"))
    val r2 = (for {
      _ <- Rule.idLens := RuleId(s"$TestId-r2")
      _ <- (Rule.nameLens >=> RuleName.valueLens) := "r2"
      _ <- Rule.createdValueLens %== (_.plus(343434))
    } yield {}).exec(r1)

    val initialQps = DesiredToggledQps(10000)
    val updatedQps = DesiredToggledQps.valueLens.mod(_ + 333, initialQps)

    val vrId1 = VersionRuleId(v.id, r1.id)
    val vrId2 = VersionRuleId(v.id, r2.id)

    val summary = VersionRuleSummary(
      vrId2,
      r2.trafficType,
      updatedQps,
      r2.created,
      r2.name)
    val expectedVersionSummary =
      VersionSummary.fromVersionAndSummaries(
        v,
        List(summary))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r1)) {
                p =>
                  for {
                    _ <- p.addRuleToVersion(v.id, r1.id, summary.desiredQps)
                    updateResult <- p.updateRuleInVersion(vrId1, r2)
                    actualSummary <- p.getVersionSummary(v.id)
                    actualRule <- p.getRule(r2.id)

                    _ <- p.removeRuleFromVersion(vrId1)
                    _ <- p.removeRuleFromVersion(vrId2)
                    _ <- p.deleteRule(r2.id)

                  } yield
                  (updateResult must equal(r2.right)) and
                    (actualSummary must equal(Option(expectedVersionSummary))) and
                    (actualRule must equal(Option(r2)))
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules update rule on version fails for published Version" ! {
    val TestId = "0513"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r = sampleRule(TestId, RuleName("r"))
    val vrId = VersionRuleId(v.id, r.id)
    val expected =
      ConfigurationError.versionAlreadyPublished(v.id).left

    val publishInstant = PublishedInstant(new Instant(513))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              p =>
                for {
                  vs <- p.publishVersion(v.id, publishInstant)
                  addResult <- p.updateRuleInVersion(vrId, r)
                } yield addResult must equal(expected)
            }
          }
        }
      }
    }
  }
  "Versions/Rules updateRuleInVersion fails for rule exists" ! {
    val TestId = "0514"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r1 = sampleRule(TestId, RuleName("r1"))
    val r2 = (for {
      _ <- Rule.idLens := RuleId(s"$TestId-r2")
      _ <- (Rule.nameLens >=> RuleName.valueLens) := "r2"
      _ <- Rule.createdValueLens %== (_.plus(343434))
    } yield {}).exec(r1)

    val vrId1 = VersionRuleId(v.id, r1.id)
    val expected = ConfigurationError.ruleAlreadyExists(r2.id)

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r1, r2)) {
                p =>
                  for {
                    updateResult <- p.updateRuleInVersion(vrId1, r2)
                  } yield updateResult must equal(expected.left)
              }
            }
          }
        }
      }
    }
  }
  "Versions/Rules copy version" ! {
    val TestId = "0515"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val vIdNew = VersionId(s"$TestId-v-new")
    val vIdCreatedInstant = CreatedInstant(new Instant(44444))
    val vNew = Version(
      vIdNew,
      dpCfg.id,
      vIdCreatedInstant,
      ModifiedInstant(vIdCreatedInstant.value),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r1 = sampleRule(TestId, RuleName("r1"))
    val ruleQps = DesiredToggledQps(10000)

    val vrId = VersionRuleId(v.id, r1.id)

    val vrs = VersionRuleSummary(
      VersionRuleId(vIdNew, r1.id),
      r1.trafficType,
      ruleQps,
      r1.created,
      r1.name)

    val expected =
      VersionSummary.fromVersionAndSummaries(
        vNew,
        List(vrs)
      )

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r1)) {
                p =>
                  for {
                    _ <- p.addRuleToVersion(
                      v.id,
                      r1.id,
                      ruleQps)
                    newVS <- p.copyVersion(v.id, vIdNew, vIdCreatedInstant)
                    actual <- p.getVersionSummary(vIdNew)

                    _ <- p.removeRuleFromVersion(vrId)
                    _ <- p.removeRuleFromVersion(VersionRuleId(vIdNew, r1.id))
                    _ <- p.deleteVersion(vIdNew)

                  } yield
                  newVS must equal(expected.right) and
                    (actual must equal(Option(expected)))
              }
            }
          }
        }
      }
    }
  }
  "Versions/Rules copy version fails when source version does not exist" ! {
    val TestId = "0515a"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val otherVId = VersionId.valueLens.mod(_ + "undefined", v.id)

    val vIdNew = VersionId(s"$TestId-v-new")
    val vIdCreatedInstant = CreatedInstant(new Instant(44444))
    val vNew = Version(
      vIdNew,
      dpCfg.id,
      vIdCreatedInstant,
      ModifiedInstant(vIdCreatedInstant.value),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r1 = sampleRule(TestId, RuleName("r1"))
    val ruleQps = DesiredToggledQps(10000)

    val vrId = VersionRuleId(v.id, r1.id)

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r1)) {
                p =>
                  for {
                    _ <- p.addRuleToVersion(
                      v.id,
                      r1.id,
                      ruleQps)
                    copyResult <- p.copyVersion(otherVId, vIdNew, vIdCreatedInstant)
                    actual <- p.getVersionSummary(vIdNew)

                    _ <- p.removeRuleFromVersion(vrId)
                    _ <- p.deleteVersion(vIdNew)
                  } yield
                  (copyResult must equal(
                    ConfigurationError.versionNotFound(otherVId).left)) and
                    (actual must equal(Option.empty))
              }
            }
          }
        }
      }
    }
  }
  "Versions/Rules copy version fails when new version already exists" ! {
    val TestId = "0515b"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val v1 = Version(
      VersionId(s"$TestId-v1"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val v2 = Version(
      VersionId(s"$TestId-v2"),
      dpCfg.id,
      CreatedInstant(new Instant(44444)),
      ModifiedInstant(new Instant(44444)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val otherInstant = CreatedInstant.valueLens.mod(_.plus(1231), v2.created)

    val r1 = sampleRule(TestId, RuleName("r1"))
    val ruleQps = DesiredToggledQps(10000)

    val vrId = VersionRuleId(v1.id, r1.id)

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v1.id, v1.created, v1.maxQps) {
              usingV(v2.id, dpCfg.id, v2.created, v2.maxQps) {
                usingRules(List(r1)) {
                  p =>
                    for {
                      _ <- p.addRuleToVersion(
                        v1.id,
                        r1.id,
                        ruleQps)

                      copyResult <- p.copyVersion(v1.id, v2.id, otherInstant)
                      actual <- p.getVersion(v2.id)

                      _ <- p.removeRuleFromVersion(vrId)
                    } yield
                    (copyResult must equal(
                      ConfigurationError.versionAlreadyExists(v2.id).left)) and
                      (actual must equal(Option(v2)))
                }
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules set QPS on version" ! {
    val TestId = "0516"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val newMaxQps = DemandPartnerMaxQps(MaxQps(19))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val modified = ModifiedInstant(new Instant(99999))

    val vNewQps = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      modified,
      None,
      newMaxQps)

    val r1 = sampleRule(TestId, RuleName("r1"))
    // set modify created instant for deterministic ordering of summaries
    val r2 = (Rule.createdLens >=> RuleCreatedInstant.valueLens)
      .mod(_.minus(1234), sampleRule(TestId, RuleName("r2")))
    val initialQps = DesiredToggledQps(10000)
    val newRuleQps = DesiredToggledQps.valueLens.mod(_ + 8745, initialQps)

    val vrs1Updated = VersionRuleSummary(
      VersionRuleId(v.id, r1.id),
      r1.trafficType,
      newRuleQps,
      r1.created,
      r1.name)
    val vrs2 = VersionRuleSummary(
      VersionRuleId(v.id, r2.id),
      r1.trafficType,
      initialQps,
      r2.created,
      r2.name)

    val expectedSummary =
      VersionSummary.fromVersionAndSummaries(
        vNewQps,
        List(vrs1Updated, vrs2))

    val rulesUpdate = Map(r1.id -> newRuleQps)
    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r1, r2)) {
                p =>
                  for {
                    _ <- p.addRuleToVersion(
                      vrs1Updated.id.versionId,
                      vrs1Updated.id.ruleId,
                      initialQps)
                    _ <- p.addRuleToVersion(
                      vrs2.id.versionId,
                      vrs2.id.ruleId,
                      initialQps)

                    updateResult <- p.setVersionQps(v.id, newMaxQps, rulesUpdate, modified)
                    actual <- p.getVersionSummary(vNewQps.id)
                    _ <- p.removeRuleFromVersion(vrs1Updated.id)
                    _ <- p.removeRuleFromVersion(vrs2.id)
                  } yield
                  updateResult must equal(expectedSummary.right) and
                    (actual must equal(Option(expectedSummary)))
              }
            }
          }
        }
      }
    }
  }
  "Versions/Rules set QPS fails to when version not found" ! {
    val TestId = "0517"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val newMaxQps = DemandPartnerMaxQps(MaxQps(19))

    val modified = ModifiedInstant(new Instant(99999))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))
    val unknownVId = VersionId("unknown")

    val r1 = sampleRule(TestId, RuleName("r1"))
    val initialQps = DesiredToggledQps(10000)
    val newRuleQps = DesiredToggledQps.valueLens.mod(_ - 11442, initialQps)
    val rulesUpdate = Map(r1.id -> newRuleQps)
    val v1r1 = VersionRuleSummary(
      VersionRuleId(
        v.id,
        r1.id),
      r1.trafficType,
      initialQps,
      r1.created,
      r1.name)
    val expectedVs = VersionSummary(
      v.id,
      dpCfg.id,
      v.created,
      v.modified,
      v.published,
      v.maxQps,
      List(v1r1))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r1)) {
                p =>
                  for {
                    _ <- p.addRuleToVersion(
                      v1r1.id.versionId,
                      v1r1.id.ruleId,
                      v1r1.desiredQps)
                    badV <- p.setVersionQps(unknownVId, newMaxQps, rulesUpdate, modified)
                    //get summary to ensure no update was made
                    actualVs <- p.getVersionSummary(v.id)
                    _ <- p.removeRuleFromVersion(v1r1.id)
                  } yield
                  (badV must equal(
                    ConfigurationError.versionNotFound(unknownVId).left)) and
                    (actualVs must equal(
                      Option(expectedVs)))
              }
            }
          }
        }
      }
    }
  }
  "Versions/Rules set QPS for version fails when rule not associated with Version" ! {
    val TestId = "0518"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id),
      ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val newMaxQps = DemandPartnerMaxQps(MaxQps(19))
    val modified = ModifiedInstant(new Instant(99999))

    val v = Version(
      VersionId(s"$TestId-v"),
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      None,
      DemandPartnerMaxQps(MaxQps(345366)))

    val r1 = sampleRule(TestId, RuleName("r1"))
    val initialQps = DesiredToggledQps(10000)
    val newRuleQps = DesiredToggledQps.valueLens.mod(_ - 2526, initialQps)

    val badRulesUpdate = Map(r1.id -> newRuleQps)
    val expectedVs = VersionSummary(
      v.id,
      dpCfg.id,
      v.created,
      v.modified,
      v.published,
      v.maxQps,
      List.empty)

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r1)) {
                p =>
                  for {
                    badR <- p.setVersionQps(v.id, newMaxQps, badRulesUpdate, modified)
                    //get summary to ensure no update was made
                    actualVs <- p.getVersionSummary(v.id)
                  } yield
                  (badR must equal(
                    ConfigurationError.rulesNotInVersion(Set(r1.id)).left)) and
                    (actualVs must equal(Option(expectedVs)))
              }
            }
          }
        }
      }
    }
  }

  "Versions/Rules set QPS for version fails when Version already published" ! {
    val TestId = "0519"
    val dp = sampleDemandPartner(
      DemandPartnerId(s"$TestId-dp"))

    val sp = sampleSupplyPartner(
      SupplyPartnerId(s"$TestId-sp"))

    val dpCfg = RouterConfiguration(
      RouterConfigurationId(dp.id, sp.id), ConfigurationEndpoint("ntoggle.com", Port(4000)))

    val newMaxQps = DemandPartnerMaxQps(MaxQps(19))

    val publishedInstant = PublishedInstant(new Instant(513))

    val modified = ModifiedInstant(new Instant(99999))

    val vId = VersionId(s"$TestId-v")
    val v = Version(
      vId,
      dpCfg.id,
      CreatedInstant(new Instant(33333)),
      ModifiedInstant(new Instant(33333)),
      Some(publishedInstant),
      DemandPartnerMaxQps(MaxQps(345366)))

    val r1 = sampleRule(TestId, RuleName("r1"))
    val initialQps = DesiredToggledQps(10000)
    val newRuleQps = DesiredToggledQps.valueLens.mod(_ - 2526, initialQps)

    val rulesQpsUpdate = Map(r1.id -> newRuleQps)

    val vrs = VersionRuleSummary(
      VersionRuleId(v.id, r1.id),
      r1.trafficType,
      newRuleQps,
      r1.created,
      r1.name)

    val expectedVs = VersionSummary(
      v.id,
      dpCfg.id,
      v.created,
      v.modified,
      v.published,
      v.maxQps,
      List(vrs))

    usingClient {
      usingPersistence {
        usingDPs(List(dp)) {
          usingSPs(List(sp)) {
            usingRC(dpCfg, v.id, v.created, v.maxQps) {
              usingRules(List(r1)) {
                p =>
                  for {
                    _ <- p.addRuleToVersion(
                      vrs.id.versionId,
                      vrs.id.ruleId,
                      vrs.desiredQps)
                    vs <- p.publishVersion(v.id, publishedInstant)
                    badUpdate <- p.setVersionQps(v.id, newMaxQps, rulesQpsUpdate, modified)
                    //get summary to ensure no update was made
                    actualVs <- p.getVersionSummary(v.id)
                    _ <- p.removeRuleFromVersion(vrs.id)

                  } yield
                  (badUpdate must equal(
                    ConfigurationError.versionAlreadyPublished(vId).left)) and
                    (actualVs must equal(Option(expectedVs)))
              }
            }
          }
        }
      }
    }
  }

  def sampleRule(testId: String, ruleName: RuleName): Rule =
    Rule(
      RuleId(s"$testId-${ruleName.value}"),
      ruleName,
      RuleCreatedInstant(new Instant(testId.hashCode)),
      Mobile,
      RuleConditions(
        Option(
          RuleCondition(
            DefaultConditionAction.Allow,
            Set(CarrierId(s"$testId")),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Exclude,
            Set(HandsetId(
              HandsetManufacturer(s"$testId-${ruleName.value}-MFG"),
              HandsetModel(s"$testId-${ruleName.value}-Model"))),
            UndefinedConditionAction.Allow)),
        Option(
          RuleCondition(
            DefaultConditionAction.Allow,
            Set(HasDeviceId),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Exclude,
            Set(OsId(
              OsName(s"$testId-${ruleName.value}-OsName"),
              OsVersion(s"$testId-${ruleName.value}-Version"))),
            UndefinedConditionAction.Allow)),
        Option(
          RuleCondition(
            DefaultConditionAction.Exclude,
            Set(Wifi),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Exclude,
            Set(HasIpAddress),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Allow,
            Set(
              AppId(s"$testId-${ruleName.value}-1"),
              AppId(s"$testId-${ruleName.value}-2")),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Exclude,
            Set(IsBanner),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Exclude,
            Set(IsMraid),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Exclude,
            Set(NotInterstitial),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Exclude,
            Set(NotVideo),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Exclude,
            Set(IsNative),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Exclude,
            Set(AdSizeId(AdSizeWidth(1024), AdSizeHeight(768))),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Exclude,
            Set.empty,
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Allow,
            Set(CountryIdAlpha2("US")),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Exclude,
            Set(RegionId.fromStringKey("US-FL")),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Allow,
            Set(CityName(s"$testId-${ruleName.value}-City∆¨ˆºµ≈∫Ω")),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Allow,
            Set(ZipAlpha(s"$testId-${ruleName.value}-Zip")),
            UndefinedConditionAction.Exclude)),
        Option(
          RuleCondition(
            DefaultConditionAction.Allow,
            Set(UserListName(s"$testId-${ruleName.value}-UserList")),
            UndefinedConditionAction.Exclude))
      )
    )

  def sampleDemandPartner(id: DemandPartnerId): DemandPartner =
    DemandPartner(
      id,
      DemandPartnerName(s"TestDemandPartner-${id.id}"))

  def sampleSupplyPartner(
    id: SupplyPartnerId) : SupplyPartner =
    SupplyPartner(id, SupplyPartnerName(s"TestSupplyPartner-${id.id}"))

  def usingDP[A](
    dp: DemandPartner)(f: SqlPersistence => Future[A]): SqlPersistence => Future[A] = {
    persistence =>
      for {
        _ <- persistence.addDemandPartner(dp).recoverWith {
          case e: UniqueConstraintViolation => Future.successful(())
        }
        result <- f(persistence).map(_.right[Exception]).recoverWith {
          case e: Exception => Future.successful(e.left)
        }
        _ <- persistence.deleteDemandPartner(dp.id).recoverWith {
          case e: Exception =>
            logger.warn("Unable to delete DemandPartner", e)
            Future.successful(e.left)
        }
        a <- result.fold(Future.failed, Future.successful)
      } yield a
  }

  def usingDPs[A](
    dps: List[DemandPartner])(f: SqlPersistence => Future[A]): SqlPersistence => Future[A] = {
    persistence =>
      for {
        _ <- dps.traverseU(dp =>
          persistence.addDemandPartner(dp).recoverWith {
            case e: UniqueConstraintViolation => Future.successful(())
          })
        result <- f(persistence).map(_.right[Exception]).recoverWith {
          case e: Exception => Future.successful(e.left)
        }
        _ <- dps.traverseU(dp => persistence.deleteDemandPartner(dp.id).recoverWith {
          case e: Exception =>
            logger.warn("Unable to delete DemandPartner", e)
            Future.successful(e.left)
        })
        a <- result.fold(Future.failed, Future.successful)
      } yield a
  }

  def usingSP[A](
    sp: SupplyPartner)(f: SqlPersistence => Future[A]): SqlPersistence => Future[A] = {
    persistence =>
      for {
        _ <- persistence.addSupplyPartner(sp).recoverWith {
          case e: UniqueConstraintViolation => Future.successful(())
        }
        result <- f(persistence).map(_.right[Exception]).recoverWith {
          case e: Exception => Future.successful(e.left)
        }
        _ <- persistence.deleteSupplyPartner(sp.id).recoverWith {
          case e: Exception =>
            logger.warn("Unable to delete SupplyPartner", e)
            Future.successful(e.left)
        }
        a <- result.fold(Future.failed, Future.successful)
      } yield a
  }

  def usingSPs[A](
    sps: List[SupplyPartner])(f: SqlPersistence => Future[A]): SqlPersistence => Future[A] = {
    persistence =>
      for {
        _ <- sps.traverseU(sp => persistence.addSupplyPartner(sp).recoverWith {
          case e: UniqueConstraintViolation => Future.successful(())
        })
        result <- f(persistence).map(_.right[Exception]).recoverWith {
          case e: Exception => Future.successful(e.left)
        }
        _ <- sps.traverseU(sp => persistence.deleteSupplyPartner(sp.id).recoverWith {
          case e: Exception =>
            logger.warn("Unable to delete SupplyPartner", e)
            Future.successful(e.left)
        })
        a <- result.fold(Future.failed, Future.successful)
      } yield a
  }

  def usingRC[A](
    rc: RouterConfiguration,
    initialVersion: VersionId,
    created: CreatedInstant,
    initialMaxQps: DemandPartnerMaxQps)(f: SqlPersistence => Future[A]): SqlPersistence => Future[A] = {
    persistence =>
      for {
        _ <- persistence.addRouterConfiguration(
          rc,
          initialVersion,
          created, initialMaxQps).recoverWith {
          case e: UniqueConstraintViolation => Future.successful(())
        }
        result <- f(persistence).map(_.right[Exception]).recoverWith {
          case e: Exception => Future.successful(e.left)
        }
        _ <- persistence.deleteVersion(initialVersion).recoverWith {
          case e: Exception =>
            logger.warn("Unable to delete Version", e)
            Future.successful(e.left)
        }
        _ <- persistence.deleteRouterConfiguration(rc.id).recoverWith {
          case e: Exception =>
            logger.warn("Unable to delete RouterConfiguration", e)
            Future.successful(e.left)
        }

        a <- result.fold(Future.failed, Future.successful)
      } yield a
  }

  def usingRcNoDelete[A](
    rc: RouterConfiguration,
    initialVersion: VersionId,
    created: CreatedInstant,
    initialMaxQps: DemandPartnerMaxQps)(f: SqlPersistence => Future[A]): SqlPersistence => Future[A] = {
    persistence =>
      for {
        _ <- persistence.addRouterConfiguration(
          rc,
          initialVersion,
          created, initialMaxQps).recoverWith {
          case e: UniqueConstraintViolation => Future.successful(())
        }
        result <- f(persistence).map(_.right[Exception]).recoverWith {
          case e: Exception => Future.successful(e.left)
        }
        a <- result.fold(Future.failed, Future.successful)
      } yield a
  }

  def usingV[A](
    id: VersionId,
    rcId: RouterConfigurationId,
    created: CreatedInstant,
    initialMaxQps: DemandPartnerMaxQps)(f: SqlPersistence => Future[A]): SqlPersistence => Future[A] = {
    persistence =>
      for {
        _ <- persistence.addVersion(id, rcId, created, initialMaxQps).recoverWith {
          case e: UniqueConstraintViolation => Future.successful(())
        }
        result <- f(persistence).map(_.right[Exception]).recoverWith {
          case e: Exception => Future.successful(e.left)
        }
        _ <- persistence.deleteVersion(id).recoverWith {
          case e: Exception =>
            logger.warn("Unable to delete Version", e)
            Future.successful(e.left)
        }
        a <- result.fold(Future.failed, Future.successful)
      } yield a
  }

  def usingRules[A](
    rules: List[Rule])(f: SqlPersistence => Future[A]): SqlPersistence => Future[A] = {
    persistence =>
      for {
        _ <- rules.traverseU(r => persistence.addRule(r).recoverWith {
          // Just in case
          case e: ForeignKeyViolationError => Future.successful(())
          case e: UniqueConstraintViolation => Future.successful(())
        })
        result <- f(persistence).map(_.right[Exception]).recoverWith {
          case e: Exception => Future.successful(e.left)
        }
        _ <- rules.traverseU(r => persistence.deleteRule(r.id).recoverWith {
          case e: Exception =>
            logger.warn("Unable to delete Rule", e)
            Future.successful(e.left)
        })
        a <- result.fold(Future.failed, Future.successful)
      } yield a
  }

  // provide cleanup for rules that were not added by the Using functions.
  def usingRulesToClean[A](
    rules: List[Rule])(f: SqlPersistence => Future[A]): SqlPersistence => Future[A] = {
    persistence =>
      for {
        result <- f(persistence).map(_.right[Exception]).recoverWith {
          case e: Exception => Future.successful(e.left)
        }

        _ <- rules.traverseU(r => persistence.deleteRule(r.id).recoverWith {
          case e: Exception =>
            logger.warn("Unable to delete Rule", e)
            Future.successful(e.left)
        })

        a <- result.fold(Future.failed, Future.successful)
      } yield a

  }

  def usingPersistence[A](f: SqlPersistence => A): SqlClient => A =
    client => {
      val persistence = new SqlPersistence(client, newConditionId)
      f(persistence)
    }

  def usingClient[A](f: SqlClient => Future[A]): A = {
    val client = dbConfig.newSqlClient()
    try {
      ScalaFuture.await(timeout) {
        f(client)
      }
    } finally {
      client.shutdown()
    }
  }

}
