package com.ntoggle.kubitschek
package infra

import java.util.UUID
import scalaz._

case class ParamExtractorError(name: String, actual: String)

object ParamExtractor {
  def from[A](f: String => ParamExtractorError \/ A): ParamExtractor[A] = new ParamExtractor[A] {
    def extract(s: String): ParamExtractorError \/ A = f(s)
  }
  def apply[A: ParamExtractor]: ParamExtractor[A] = implicitly[ParamExtractor[A]]

  implicit lazy val monadPathExtractor: Monad[ParamExtractor] = new Monad[ParamExtractor] {
    def point[A](a: => A): ParamExtractor[A] = ParamExtractor.from(_ => \/-(a))
    def bind[A, B](
      fa: ParamExtractor[A])(f: A => ParamExtractor[B]): ParamExtractor[B] =
      ParamExtractor.from { s =>
        for {
          a <- fa.extract(s)
          b <- f(a).extract(s)
        } yield b
      }
  }

  object ExpectedBasicTypes {
    val UUID = "UUID"
    val INT = "Int"
  }

  implicit val peUUID: ParamExtractor[UUID] = ParamExtractor.from { s =>
    try {
      \/-(UUID.fromString(s))
    } catch {
      case _: IllegalArgumentException =>
        -\/(ParamExtractorError(ExpectedBasicTypes.UUID, s))
    }
  }
  implicit val peString: ParamExtractor[String] = ParamExtractor.from { s =>
    \/-(s)
  }
  implicit val peInt: ParamExtractor[Int] = ParamExtractor.from { s =>
    try \/-(s.toInt)
    catch {
      case _: IllegalArgumentException =>
        -\/(ParamExtractorError(ExpectedBasicTypes.INT, s))
    }
  }
}
trait ParamExtractor[A] {
  def extract(s: String): ParamExtractorError \/ A
}
