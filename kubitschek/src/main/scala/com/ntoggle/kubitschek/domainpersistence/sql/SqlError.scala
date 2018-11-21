package com.ntoggle.kubitschek.domainpersistence.sql

import java.sql.SQLException

import scala.util.matching.Regex

case class ConstraintName(value: String) {
  def nameMatches(regex: Regex): Boolean =
    value.matches(regex.regex)
}

class UniqueConstraintViolation(
  val msg: String,
  val constraint: Option[ConstraintName],
  val cause: Option[Throwable])
  extends Exception(msg, cause.orNull)
  with SqlError

class ForeignKeyViolationError(
  val msg: String,
  val constraint: Option[ConstraintName],
  val cause: Option[Throwable])
  extends Exception(msg, cause.orNull)
  with SqlError

class NumericValueOutOfRangeError(
  val msg: String,
  val cause: Option[SQLException])
  extends Exception(msg, cause.orNull)
  with SqlError

class NumericConstantOutOfRangeError(
  val msg: String,
  val cause: Option[SQLException])
  extends Exception(msg, cause.orNull)
  with SqlError

class BadDateTimeError(
  val msg: String,
  val cause: Option[Throwable])
  extends Exception(msg, cause.orNull)
  with SqlError

sealed trait SqlError {
  this: Exception =>
  def ex: Exception = this
}
