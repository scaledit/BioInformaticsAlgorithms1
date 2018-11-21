package com.ntoggle.kubitschek
package domain

import com.ntoggle.goldengate.playjson.test.JsonTests
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers

import scalaz.scalacheck.ScalazProperties

class RouterConfigurationIdSpec
  extends Specification
  with ScalaCheck
  with JsonTests
  with ScalazMatchers {
  import DomainGenerators._

  "RouterConfigurationId.order" ! ScalazProperties.order.laws[RouterConfigurationId]
  "RouterConfigurationId.format" ! checkFormat[RouterConfigurationId]

}
