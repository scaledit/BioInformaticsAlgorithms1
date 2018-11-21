package com.ntoggle.kubitschek
package domain

import com.ntoggle.goldengate.jodaext.Implicits._

import org.joda.time.Instant
import play.api.libs.json.Format
import play.api.libs.functional.syntax._

import scalaz.{Lens, @>, Order}

case class CreatedInstant(value: Instant) extends AnyVal
object CreatedInstant {
  implicit val CreatedInstantOrder: Order[CreatedInstant] =
    Order[Instant].contramap(_.value)
  import InstantJson._
  implicit val format: Format[CreatedInstant] =
  implicitly[Format[Instant]].inmap(CreatedInstant.apply, _.value)

  val valueLens: CreatedInstant @> Instant =
    Lens.lensu((ci, v) => ci.copy(value = v), _.value)
}
