package com.ntoggle.kubitschek.domain

import com.ntoggle.goldengate.playjson.test.JsonTests
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers

import scalaz.scalacheck.ScalazProperties

class VersionIdSpec
  extends Specification
  with ScalaCheck
  with JsonTests
  with ScalazMatchers {
  import DomainGenerators._

  "VersionId.order" ! ScalazProperties.order.laws[VersionId]
  "VersionId.format" ! checkFormat[VersionId]

}
