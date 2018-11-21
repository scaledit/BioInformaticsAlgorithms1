package com.ntoggle.kubitschek.domain

import org.joda.time.Instant
import play.api.libs.json._

object InstantJson {

  import play.api.data.validation._

  implicit val instantReads: Reads[Instant] = Reads[Instant]({
    case JsNumber(bd) =>
      val millis = bd.longValue // Note: Can lose precision. May wish to return JsError if out of range.
    val instant = new Instant(millis)
      JsSuccess(instant)
    case _ => JsError(ValidationError("error.expected.jsnumber"))
  })

  implicit val instantWrites: Writes[Instant] = Writes[Instant] { instant: Instant =>
    JsNumber(instant.getMillis)
  }

  implicit val instantFormat: Format[Instant] = Format[Instant](instantReads, instantWrites)

}

