package com.ntoggle.kubitschek
package domain

import com.ntoggle.goldengate.jodaext.Implicits._

import org.joda.time.Instant
import play.api.libs.json.Format
import play.api.libs.functional.syntax._

import scalaz.{Lens, @>, Order}

case class ModifiedInstant(value: Instant) extends AnyVal
object ModifiedInstant {
  implicit val ModifiedInstantOrder: Order[ModifiedInstant] =
    Order[Instant].contramap(_.value)
  import InstantJson._
  implicit val format: Format[ModifiedInstant] =
    implicitly[Format[Instant]].inmap(ModifiedInstant.apply, _.value)

  val valueLens: ModifiedInstant @> Instant =
    Lens.lensu((ci, v) => ci.copy(value = v), _.value)
}

