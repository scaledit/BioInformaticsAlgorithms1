package com.ntoggle.kubitschek
package domainpersistence
package config

import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import scala.concurrent.ExecutionContext

trait PersistenceConfig {
  def persistence(implicit ctx: ExecutionContext): Persistence
}

object PersistenceConfig {
  def fromConfig(c: Config): PersistenceConfig = {
    c.getString("type") match {

      case "memory-empty" => EmptyMemoryPersistenceConfig()

      case "memory" => MemoryPersistenceConfig()

      case "postgresql" => PostgresPersistenceConfig(c)

      case other => throw new ConfigException.BadValue("type", s"invalid persistence type '$other'")
    }
  }
}
