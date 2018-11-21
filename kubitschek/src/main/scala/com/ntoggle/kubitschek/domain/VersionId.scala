package com.ntoggle.kubitschek
package domain

import com.ntoggle.goldengate.playjson.FormatAsString
import com.ntoggle.goldengate.scalazint.{Showz, OrderzByString}

import scalaz.{Lens, @>}

case class VersionId(value: String) extends AnyVal
object VersionId
  extends FormatAsString[VersionId]
  with OrderzByString[VersionId]
  with Showz[VersionId] {
  val valueLens: VersionId @> String =
    Lens.lensu((id, v) => id.copy(value = v), _.value)
}

