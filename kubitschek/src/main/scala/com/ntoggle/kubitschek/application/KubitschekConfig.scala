package com.ntoggle.kubitschek
package application

import com.ntoggle.goldengate.elasticsearch.ESConfig
import com.ntoggle.kubitschek.domainpersistence.config.PersistenceConfig
import com.ntoggle.kubitschek.integration.StormPathClientConfig
import com.ntoggle.kubitschek.routes.ApiDocConfig
import com.typesafe.config.Config


object HttpServiceConfig {
  def fromConfig(config: Config): HttpServiceConfig =
    HttpServiceConfig(
      config.getString("interface"),
      config.getInt("port"))
}
case class HttpServiceConfig(interface: String, port: Int)

object KubitschekConfig {

  def fromConfig(config: Config): KubitschekConfig = {
    new KubitschekConfig(
      HttpServiceConfig.fromConfig(config.getConfig("http-service")),
      PersistenceConfig.fromConfig(config.getConfig("persistence")),
      ApiDocConfig(config.getString("api-doc.root")),
      StormPathClientConfig.fromConfig(config.getConfig("storm-path")),
      FeatureIndexConfig.fromConfig(config.getConfig("features")),
      HttpServiceConfig.fromConfig(config.getConfig("metrics-service")),
      ESConfigParser.fromConfig(config.getConfig("es")),
      EstimationConfig.fromConfig(config.getConfig("estimation")))
  }

}
class KubitschekConfig(
  val httpServiceConfig: HttpServiceConfig,
  val persistenceConfig: PersistenceConfig,
  val apiDocConfig: ApiDocConfig,
  val stormPathClientConfig: StormPathClientConfig,
  val featureConfig: FeatureIndexConfig,
  val metricsConfig: HttpServiceConfig,
  val esConfig: ESConfig,
  val estimationConfig: EstimationConfig)
