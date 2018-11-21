package com.ntoggle.kubitschek
package domain

import com.ntoggle.goldengate.jodaext.Implicits._

import org.joda.time.Instant
import play.api.libs.json.Format
import play.api.libs.functional.syntax._

import scalaz.Order

case class PublishedInstant(value: Instant) extends AnyVal
object PublishedInstant {
  implicit val publishedInstantOrder: Order[PublishedInstant] =
    Order[Instant].contramap(_.value)

  import InstantJson._
  implicit val format: Format[PublishedInstant] =
    implicitly[Format[Instant]].inmap(PublishedInstant.apply, _.value)

  def now = PublishedInstant(Instant.now())
}
