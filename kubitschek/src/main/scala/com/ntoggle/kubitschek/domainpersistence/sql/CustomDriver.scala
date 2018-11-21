package com.ntoggle.kubitschek.domainpersistence.sql

import com.ntoggle.goldengate.slick.SlickDriver
import slick.driver.PostgresDriver
import com.github.tminglei.slickpg._

class InvalidPgJsonError(msg: String) extends Exception(msg)

trait PostgresSlickDriver extends SlickDriver {
  lazy val driver = CustomPostgresDriver
}

/**
 * See https://github.com/tminglei/slick-pg
 */
trait CustomPostgresDriver
  extends PostgresDriver
  with PgArraySupport
  with PgDateSupportJoda
  with PgRangeSupport
  with PgHStoreSupport
  with PgPlayJsonSupport
  with PgSearchSupport {
  override val pgjson = "jsonb" //to keep back compatibility, pgjson's value was "json" by default

  override lazy val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  trait ImplicitsPlus
    extends Implicits
    with ArrayImplicits
    with JodaDateTimePlainImplicits
    with RangeImplicits
    with HStoreImplicits
    with PlayJsonPlainImplicits
    with SearchImplicits

  trait SimpleQLPlus
    extends SimpleQL
    with SimpleArrayPlainImplicits
    with ImplicitsPlus
    with SearchAssistants
}

object CustomPostgresDriver extends CustomPostgresDriver