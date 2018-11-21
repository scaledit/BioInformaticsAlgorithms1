package com.ntoggle.kubitschek.domainpersistence.config

import com.ntoggle.kubitschek.domainpersistence.mem.MemPersistence

import scala.concurrent.ExecutionContext

case class EmptyMemoryPersistenceConfig() extends PersistenceConfig {
  def persistence(implicit ctx: ExecutionContext) = MemPersistence.empty
}
