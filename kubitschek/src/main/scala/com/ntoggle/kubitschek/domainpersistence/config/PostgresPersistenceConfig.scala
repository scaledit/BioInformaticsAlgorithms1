package com.ntoggle.kubitschek
package domainpersistence
package config

import com.ntoggle.goldengate.slick.DataSourceConfig
import com.ntoggle.kubitschek.domainpersistence.sql.SqlPersistence
import com.typesafe.config.Config
import scala.concurrent.ExecutionContext

case class PostgresPersistenceConfig(dbCfg: Config)
  extends PersistenceConfig {

  val dbConfig = DataSourceConfig.fromConfig(dbCfg.getConfig("db"))
  val dbClient = dbConfig.newSqlClient()

  def persistence(implicit ctx: ExecutionContext) = SqlPersistence(dbClient, ctx)
}
