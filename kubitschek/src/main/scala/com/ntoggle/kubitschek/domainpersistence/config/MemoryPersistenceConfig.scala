package com.ntoggle.kubitschek.domainpersistence.config

import com.ntoggle.albi._
import com.ntoggle.albi.users.UserListId
import com.ntoggle.kubitschek.api.Port
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.domainpersistence.mem.MemPersistence
import org.joda.time.Instant

import scala.concurrent.ExecutionContext

// These should match data in test-data.sql

case class MemoryPersistenceConfig() extends PersistenceConfig {
  val dp1 = DemandPartner(
    DemandPartnerId("0d1a63f2-3f45-4fb1-9237-0b7cfed3d11f"),
    DemandPartnerName("TEST-DP-1"))
  val dp2 = DemandPartner(
    DemandPartnerId("3f214842-6b16-483f-b49f-d152e01c6e33"),
    DemandPartnerName("TEST-DP-2"))
  val dp5 = DemandPartner(
    DemandPartnerId("b20195b1-7811-4e4d-bdc3-f81943654564"),
    DemandPartnerName("TEST-DP-5"))

  val sp1 = SupplyPartner(
    SupplyPartnerId("9a726b7f-e36a-441f-b83a-2581c0edfcd3"),
    SupplyPartnerName("TEST-SP-1"))
  val sp2 = SupplyPartner(
    SupplyPartnerId("dc8333de-baa7-4f6b-babd-f0b1a87abbc5"),
    SupplyPartnerName("TEST-SP-2"))
  val sp3 = SupplyPartner(
    SupplyPartnerId("b72331e0-eee2-493f-92e8-6a0500294d70"),
    SupplyPartnerName("TEST-SP-3"))
  val sp5 = SupplyPartner(
    SupplyPartnerId("6289c608-5ed9-42fa-ba8b-4d4499b9e033"),
    SupplyPartnerName("TEST-SP-5"))
  val sp6 = SupplyPartner(
    SupplyPartnerId("d7f40e89-53b7-4cd9-a0a5-eda715d6d485"),
    SupplyPartnerName("TEST-SP-6"))

  val rc1 = RouterConfiguration(
    RouterConfigurationId(dp1.id, sp1.id), ConfigurationEndpoint("ntoggle.com", Port(4001)))
  val rc2 = RouterConfiguration(
    RouterConfigurationId(dp2.id, sp2.id), ConfigurationEndpoint("ntoggle.com", Port(4002)))
  val rc3 = RouterConfiguration(
    RouterConfigurationId(dp2.id, sp3.id), ConfigurationEndpoint("ntoggle.com", Port(4003)))
  val rc4 = RouterConfiguration(
    RouterConfigurationId(dp5.id, sp5.id), ConfigurationEndpoint("ntoggle.com", Port(4004)))

  val ver = Version(
    VersionId("2e83b2ea-0e9e-4d4d-a330-a1d0b9065270"),
    RouterConfigurationId(
      dp1.id,
      sp1.id),
    CreatedInstant(new Instant(1000)),
    ModifiedInstant(new Instant(1000)),
    None,
    DemandPartnerMaxQps(MaxQps(250000))
  )
  val ver2 = Version(
    VersionId("c1c43a16-9829-4863-a4e8-2c6ae99e5a6e"),
    RouterConfigurationId(
      dp2.id,
      sp2.id),
    CreatedInstant(new Instant(1002)),
    ModifiedInstant(new Instant(1002)),
    None,
    DemandPartnerMaxQps(MaxQps(250002))
  )

  val ver3 = Version(
    VersionId("76637df2-db5b-413c-95e0-cb111d1f2b88"),
    RouterConfigurationId(
      dp2.id,
      sp3.id),
    CreatedInstant(new Instant(1003)),
    ModifiedInstant(new Instant(1003)),
    None,
    DemandPartnerMaxQps(MaxQps(250003))
  )

  val ver4 = Version(
    VersionId("06ea485d-a566-4bb9-ae1c-24aa33abfd6f"),
    RouterConfigurationId(
      dp5.id,
      sp5.id),
    CreatedInstant(new Instant(1004)),
    ModifiedInstant(new Instant(1004)),
    None,
    DemandPartnerMaxQps(MaxQps(250004))
  )

  // dp1
  val ul1 = UserList(
    UserListId("515f5f75-3a46-4f01-8d49-53ccf28457e7"),
    UserListName("auto-something"),
    DemandPartnerId("0d1a63f2-3f45-4fb1-9237-0b7cfed3d11f"),
    CreatedInstant(new Instant("1001")),
    ModifiedInstant(new Instant("1001")))

  // dp2
  val ul2 = UserList(
    UserListId("bcbfa39c-7879-498d-ae28-1a1c68b6aa35"),
    UserListName("auto-something"),
    DemandPartnerId("3f214842-6b16-483f-b49f-d152e01c6e33"),
    CreatedInstant(new Instant("1002")),
    ModifiedInstant(new Instant("1002")))

  // dp5
  val ul5 = UserList(
    UserListId("c535561b-b999-403f-acd3-f319db6e9876"),
    UserListName("auto-something"),
    DemandPartnerId("b20195b1-7811-4e4d-bdc3-f81943654564"),
    CreatedInstant(new Instant("1005")),
    ModifiedInstant(new Instant("1005")))

  def persistence(implicit ctx: ExecutionContext) = new MemPersistence(
    Map(
      dp1.id -> dp1,
      dp2.id -> dp2,
      dp5.id -> dp5
    ),
    Map(
      sp1.id -> sp1,
      sp2.id -> sp2,
      sp3.id -> sp3,
      sp5.id -> sp5,
      sp6.id -> sp6
    ),
    Map(
      rc1.id -> rc1,
      rc2.id -> rc2,
      rc3.id -> rc3,
      rc4.id -> rc4
    ),
    Map(
      ver.id -> ver,
      ver2.id -> ver2,
      ver3.id -> ver3,
      ver4.id -> ver4
    ),
    Map.empty,
    Map.empty,
    Map(
    ul1.id -> ul1,
    ul2.id -> ul2,
    ul5.id -> ul5
    )
  )
}
