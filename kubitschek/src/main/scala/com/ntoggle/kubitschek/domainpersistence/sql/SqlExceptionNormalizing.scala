package com.ntoggle.kubitschek.domainpersistence.sql

import com.ntoggle.kubitschek._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.postgresql.util.{PSQLState, PSQLException}
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{StateT, State}
import scalaz.std.string._
import scalaz.std.option._
import scalaz.syntax.equal._

trait SqlExceptionNormalizing {
  this: LazyLogging =>
  protected def exceptionHandler: Throwable =>? SqlError
  def normalizeExceptions[A](f: => A): A = try {
    f
  } catch exceptionHandler andThen {
    e =>
      logger.error(s"Sql error: '$e'")
      throw e.ex
  }

  def normalizeFutureExceptions[A](f: => Future[A])(implicit ctx: ExecutionContext): Future[A] =
    f.recoverWith {
      exceptionHandler andThen {
        e =>
          logger.error(s"Sql error: '$e'")
          Future.failed(e.ex)
      }
    }
}

/**
 * http://www.postgresql.org/docs/9.4/static/errcodes-appendix.html
 */
trait PostgresSqlExceptionNormalizing extends SqlExceptionNormalizing {
  this: LazyLogging =>
  import PostgresSqlExceptionNormalizing._
  def exceptionHandler = handlePSQLExceptions
}
object PostgresSqlExceptionNormalizing
  extends LazyLogging {

  object AdditionalStates {
    val UniqueConstraintViolation = "23505"
    val ForeignKeyConstraintViolation = "23503"
  }

  private def extractConstraint(e: PSQLException): Option[ConstraintName] =
    for {
      msg <- Option(e.getServerErrorMessage)
      c <- Option(msg.getConstraint)
    } yield ConstraintName(c)


  val handlePSQLExceptions: Throwable =>? SqlError = {
    case e: PSQLException
      if e.getSQLState === PSQLState.NUMERIC_VALUE_OUT_OF_RANGE.getState =>
      new NumericValueOutOfRangeError(e.getMessage, Some(e))

    case e: PSQLException
      if e.getSQLState === PSQLState.NUMERIC_CONSTANT_OUT_OF_RANGE.getState =>
      new NumericConstantOutOfRangeError(e.getMessage, Some(e))

    case e: PSQLException
      if e.getSQLState === PSQLState.BAD_DATETIME_FORMAT.getState =>
      new BadDateTimeError(e.getMessage, Some(e))

    case e: PSQLException
      if e.getSQLState === AdditionalStates.UniqueConstraintViolation =>

      new UniqueConstraintViolation(
        e.getMessage,
        extractConstraint(e),
        Some(e))

    case e: PSQLException
      if e.getSQLState === AdditionalStates.ForeignKeyConstraintViolation =>

      new ForeignKeyViolationError(
        e.getMessage,
        extractConstraint(e),
        Some(e))

    case e: PSQLException =>
      // Log unhanded Exception

      import scalaz.syntax.traverse._
      def buildMessage: State[String, Unit] = for {
        _ <- State.modify[String](_ + s", sqlState: '${e.getSQLState}'")
        _ <- Option(e.getServerErrorMessage).traverseS { msg =>
          for {
            _ <- Option(msg.getDatatype)
              .traverseS(v => State.modify[String](_ + s", data type: '$v'"))

            _ <- Option(msg.getConstraint)
              .traverseS(v => State.modify[String](_ + s", constraint: '$v'"))

            _ <- Option(msg.getColumn)
              .traverseS(v => State.modify[String](_ + s", column: '$v'"))
          } yield ()
        }
      } yield ()
      val msg: String = buildMessage.exec(s"Unknown PSQLException")

      logger.error(msg, e)
      throw e
  }

}