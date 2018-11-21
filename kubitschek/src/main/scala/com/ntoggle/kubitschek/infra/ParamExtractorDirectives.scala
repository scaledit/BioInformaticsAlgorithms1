package com.ntoggle.kubitschek
package infra

import akka.http.scaladsl.server._

import scalaz.{-\/, \/-, \/}

import akka.http.scaladsl.server.directives.{RouteDirectives, BasicDirectives}

import scala.collection.immutable


trait ParamExtractorDirectives extends ToNameReceptacleEnhancer {
  import ParamExtractorDirectives._

  /**
   * Extracts the request's query parameters as a ``Map[String, String]``.
   */
  def queryParamMap: Directive1[Map[String, String]] = _parameterMap

  /**
   * Extracts the request's query parameters as a ``Map[String, List[String]]``.
   */
  def queryParamMultiMap: Directive1[Map[String, List[String]]] = _parameterMultiMap

  /**
   * Extracts the request's query parameters as a ``Seq[(String, String)]``.
   */
  def queryParamSeq: Directive1[immutable.Seq[(String, String)]] = _parameterSeq

  /**
   * Extracts a query parameter value from the request.
   * Rejects the request if the defined query parameter matcher(s) don't match.
   */
  def queryparam(pdm: ParamMagnet): pdm.Out = pdm()

  /**
   * Extracts a number of query parameter values from the request.
   * Rejects the request if the defined query parameter matcher(s) don't match.
   */
  def queryparams(pdm: ParamMagnet): pdm.Out = pdm()

}

object ParamExtractorDirectives extends ParamExtractorDirectives {
  import BasicDirectives._

  private val _parameterMap: Directive1[Map[String, String]] =
    extract(_.request.uri.query.toMap)

  private val _parameterMultiMap: Directive1[Map[String, List[String]]] =
    extract(_.request.uri.query.toMultiMap)

  private val _parameterSeq: Directive1[immutable.Seq[(String, String)]] =
    extract(_.request.uri.query.toSeq)

  sealed trait ParamMagnet {
    type Out
    def apply(): Out
  }
  object ParamMagnet {
    implicit def apply[T](value: T)(implicit pdef: ParamDef[T]): ParamMagnet {type Out = pdef.Out} =
      new ParamMagnet {
        type Out = pdef.Out
        def apply() = pdef(value)
      }
  }

  type ParamDefAux[T, U] = ParamDef[T] {type Out = U}
  sealed trait ParamDef[T] {
    type Out
    def apply(value: T): Out
  }
  object ParamDef {
    def paramDef[A, B](f: A ⇒ B): ParamDefAux[A, B] =
      new ParamDef[A] {
        type Out = B
        def apply(value: A) = f(value)
      }

    import RouteDirectives._
    type ParamOptionExtractor[T] = ParamExtractor[Option[T]]

    private def nullAsEmpty(value: String) = if (value == null) "" else value
    private def extractParameter[A, B](f: A ⇒ Directive1[B]): ParamDefAux[A, Directive1[B]] = paramDef(f)
    private def handleParamResult[T](result: Rejection \/ T): Directive1[T] =
      result match {
        case \/-(x) ⇒ provide(x)
        case -\/(e) ⇒ reject(e)
      }
    //////////////////// "regular" parameter extraction //////////////////////

    private def peExtract[T](paramName: String, pe: ParamExtractor[T]): Directive1[Option[ParamExtractorError \/ T]] =
      extractRequestContext map { ctx =>
        ctx.request.uri.query get paramName map pe.extract
      }

    private def require[T](paramName: String) = (a: Option[ParamExtractorError \/ T]) =>
      a match {
        case None => -\/(MissingQueryParamRejection(paramName))
        case Some(aa) => aa.leftMap(QueryExtractorRejection.apply)
      }

    private def default[T](paramName: String, default: T) = (a: Option[ParamExtractorError \/ T]) =>
      a match {
        case None => \/-(default)
        case Some(aa) => aa.leftMap(QueryExtractorRejection.apply)
      }

    private def option[T](paramName: String): Option[ParamExtractorError \/ T] => QueryExtractorRejection \/ Option[T] = {
      case None => \/-(None)
      case Some(aa) => aa.bimap(QueryExtractorRejection.apply, Some(_))
    }

    implicit def forNR[T](implicit pe: ParamExtractor[T]): ParamDefAux[NameReceptacle[T], Directive1[T]] =
      extractParameter[NameReceptacle[T], T] { nr ⇒ peExtract(nr.name, pe).map(require(nr.name)).flatMap(handleParamResult) }
    implicit def forString(implicit pe: ParamExtractor[String]): ParamDefAux[String, Directive1[String]] =
      extractParameter[String, String] { nr ⇒ peExtract(nr.name, pe).map(require(nr.name)).flatMap(handleParamResult) }
    implicit def forSymbol(implicit pe: ParamExtractor[String]): ParamDefAux[Symbol, Directive1[String]] =
      extractParameter[Symbol, String] { nr ⇒ peExtract(nr.name, pe).map(require(nr.name)).flatMap(handleParamResult) }
    implicit def forNER[T]: ParamDefAux[NameExtractorReceptacle[T], Directive1[T]] =
      extractParameter[NameExtractorReceptacle[T], T] { nr ⇒ peExtract(nr.name, nr.pe).map(require(nr.name)).flatMap(handleParamResult) }
    implicit def forNOR[T](implicit pe: ParamExtractor[T]): ParamDefAux[NameOptionReceptacle[T], Directive1[Option[T]]] =
      extractParameter[NameOptionReceptacle[T], Option[T]] { nr ⇒ peExtract(nr.name, pe).map(option(nr.name)).flatMap(handleParamResult) }
    implicit def forNDR[T](implicit pe: ParamExtractor[T]): ParamDefAux[NameDefaultReceptacle[T], Directive1[T]] =
      extractParameter[NameDefaultReceptacle[T], T] { nr ⇒ peExtract(nr.name, pe).map(default(nr.name, nr.default)).flatMap(handleParamResult) }
    implicit def forNOUR[T]: ParamDefAux[NameOptionExtractorReceptacle[T], Directive1[Option[T]]] =
      extractParameter[NameOptionExtractorReceptacle[T], Option[T]] { nr ⇒ peExtract(nr.name, nr.pe).map(option(nr.name)).flatMap(handleParamResult) }
    implicit def forNDUR[T]: ParamDefAux[NameDefaultExtractorReceptacle[T], Directive1[T]] =
      extractParameter[NameDefaultExtractorReceptacle[T], T] { nr ⇒ peExtract(nr.name, nr.pe).map(default(nr.name, nr.default)).flatMap(handleParamResult) }

    //////////////////// required parameter support with Extractors ////////////////////

    private def requiredFilter[T](paramName: String, pe: ParamExtractor[T], requiredValue: Any): Directive0 =
      extractRequestContext flatMap { ctx ⇒
        (ctx.request.uri.query get paramName).map(pe.extract) match {
          case Some(x) if \/-(x) == \/-(requiredValue) ⇒ pass
          case _ ⇒ reject
        }
      }
    implicit def forRVR[T](implicit pe: ParamExtractor[T]): ParamDefAux[RequiredValueReceptacle[T], Directive0] =
      paramDef[RequiredValueReceptacle[T], Directive0] { rvr ⇒ requiredFilter(rvr.name, pe, rvr.requiredValue) }
    implicit def forRVDR[T]: ParamDefAux[RequiredValueExtractorReceptacle[T], Directive0] =
      paramDef[RequiredValueExtractorReceptacle[T], Directive0] { rvr ⇒ requiredFilter(rvr.name, rvr.pe, rvr.requiredValue) }

    //////////////////// repeated parameter support ////////////////////

    private def repeatedFilter[T](paramName: String, pe: ParamExtractor[T]): Directive1[Iterable[T]] =
      extractRequestContext flatMap { ctx =>
        provide(ctx.request.uri.query.getAll(paramName).map(pe.extract(_).toOption).flatten)
      }

    implicit def forRepVR[T](implicit pe: ParamExtractor[T]): ParamDefAux[RepeatedValueReceptacle[T], Directive1[Iterable[T]]] =
      extractParameter[RepeatedValueReceptacle[T], Iterable[T]] { rvr ⇒ repeatedFilter(rvr.name, pe) }
    implicit def forRepVDR[T]: ParamDefAux[RepeatedValueExtractorReceptacle[T], Directive1[Iterable[T]]] =
      extractParameter[RepeatedValueExtractorReceptacle[T], Iterable[T]] { rvr ⇒ repeatedFilter(rvr.name, rvr.pe) }

    //////////////////// tuple support ////////////////////

    import akka.http.scaladsl.server.util.TupleOps._
    import akka.http.scaladsl.server.util.BinaryPolyFunc

    implicit def forTuple[T](implicit fold: FoldLeft[Directive0, T, ConvertParamDefAndConcatenate.type]): ParamDefAux[T, fold.Out] =
      paramDef[T, fold.Out](fold(BasicDirectives.pass, _))

    object ConvertParamDefAndConcatenate extends BinaryPolyFunc {
      implicit def from[P, TA, TB](implicit pdef: ParamDef[P] {type Out = Directive[TB]}, ev: Join[TA, TB]): BinaryPolyFunc.Case[Directive[TA], P, ConvertParamDefAndConcatenate.type] {type Out = Directive[ev.Out]} =
        at[Directive[TA], P] { (a, t) ⇒ a & pdef(t) }
    }
  }
}
