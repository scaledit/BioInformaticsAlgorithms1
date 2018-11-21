package com.ntoggle.kubitschek
package infra

private[infra] trait ToNameReceptacleEnhancer {
  implicit def symbol2NR(symbol: Symbol) = new NameReceptacle[String](symbol.name)
  implicit def string2NR(string: String) = new NameReceptacle[String](string)
}
object ToNameReceptacleEnhancer extends ToNameReceptacleEnhancer

class NameReceptacle[T](val name: String) {
  def as[B] = new NameReceptacle[B](name)
  def as[B](pe: ParamExtractor[B]) = new NameExtractorReceptacle(name, pe)
  def ? = new NameOptionReceptacle[T](name)
  def ?[B](default: B) = new NameDefaultReceptacle(name, default)
  def ![B](requiredValue: B) = new RequiredValueReceptacle(name, requiredValue)
  def * = new RepeatedValueReceptacle[T](name)
}

class NameExtractorReceptacle[T](val name: String, val pe: ParamExtractor[T]) {
  def ? = new NameOptionExtractorReceptacle[T](name, pe)
  def ?(default: T) = new NameDefaultExtractorReceptacle(name, default, pe)
  def !(requiredValue: T) = new RequiredValueExtractorReceptacle(name, requiredValue, pe)
  def * = new RepeatedValueExtractorReceptacle[T](name, pe)
}

class NameOptionReceptacle[T](val name: String)

class NameDefaultReceptacle[T](val name: String, val default: T)

class RequiredValueReceptacle[T](val name: String, val requiredValue: T)

class RepeatedValueReceptacle[T](val name: String)

class NameOptionExtractorReceptacle[T](val name: String, val pe: ParamExtractor[T])

class NameDefaultExtractorReceptacle[T](val name: String, val default: T, val pe: ParamExtractor[T])

class RequiredValueExtractorReceptacle[T](val name: String, val requiredValue: T, val pe: ParamExtractor[T])

class RepeatedValueExtractorReceptacle[T](val name: String, val pe: ParamExtractor[T])