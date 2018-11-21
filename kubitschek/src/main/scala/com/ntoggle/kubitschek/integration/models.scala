package com.ntoggle.kubitschek.integration

import java.util.UUID

import akka.http.scaladsl.server.Rejection
import com.ntoggle.albi.{SupplyPartnerId, DemandPartnerId, SupplyPartner}
import com.ntoggle.goldengate.playjson.{PlayJsonUtils, FormatAsInt, FormatAsString}
import com.ntoggle.goldengate.scalazint.{OrderzByString, Showz, EqualzAndShowz}
import com.ntoggle.kubitschek.infra.{AuthenticationRejection, AuthorizationRejection}
import play.api.libs.json._
import play.api.libs.functional.syntax._


case class Username(value: String)
object Username
  extends EqualzAndShowz[Username]
  with FormatAsString[Username]

case class Password(value: String) {
  override def toString: String = "**********"
}
object Password
  extends EqualzAndShowz[Password]
  with FormatAsString[Password]

case class EmailAddress(value: String)
object EmailAddress
  extends EqualzAndShowz[EmailAddress]
  with FormatAsString[EmailAddress]

case class Fullname(value: String)
object Fullname
  extends EqualzAndShowz[Fullname]
  with FormatAsString[Fullname]

case class AccessToken(value: String)
object AccessToken
  extends EqualzAndShowz[AccessToken]
  with FormatAsString[AccessToken]

case class TokenType(value: String)
object TokenType
  extends EqualzAndShowz[TokenType]
  with FormatAsString[TokenType]

case class TokenExpiresInSec(value: Int)
object TokenExpiresInSec
  extends EqualzAndShowz[TokenExpiresInSec]
  with FormatAsInt[TokenExpiresInSec]

case class OAuthToken(
  accessToken: AccessToken,
  refreshToken: AccessToken,
  tokenType: TokenType,
  expiresInSec: TokenExpiresInSec)
object OAuthToken
  extends EqualzAndShowz[OAuthToken] {

  implicit def formatOAuthToken: Format[OAuthToken] =
    Json.format[OAuthToken]

}

case class StormPathOAuthToken(
  accessToken: AccessToken,
  refreshToken: AccessToken,
  tokenType: TokenType,
  expiresInSec: TokenExpiresInSec,
  accessTokenUrl: String) {

  def toOAuthToken: OAuthToken =
    OAuthToken(accessToken, refreshToken, tokenType, expiresInSec)

}

object StormPathOAuthToken
  extends EqualzAndShowz[StormPathOAuthToken]{

  object Keys {
    val AccessToken = "access_token"
    val RefreshToken = "refresh_token"
    val TokenType = "token_type"
    val ExpiresIn = "expires_in"
    val AccessTokenUrl = "stormpath_access_token_href"
  }

  implicit val reads: Reads[StormPathOAuthToken] = {
    ((__ \ Keys.AccessToken).read[AccessToken] ~
      (__ \ Keys.RefreshToken).read[AccessToken] ~
      (__ \ Keys.TokenType).read[TokenType] ~
      (__ \ Keys.ExpiresIn).read[TokenExpiresInSec] ~
      (__ \ Keys.AccessTokenUrl).read[String]
      )(StormPathOAuthToken.apply _)
  }

  def None = StormPathOAuthToken(null, null, null, null, null)

}

// hardcode this for now
case class UserPreferences(
  language: String,
  timezone: String)
object UserPreferences
  extends EqualzAndShowz[UserPreferences] {

  implicit def formatUserPrefs: Format[UserPreferences] =
    Json.format[UserPreferences]

  val Default = UserPreferences("en-US", "America/New_York")
}

case class DemandConfiguration(
  supplyPartners: List[(SupplyPartner)]
  )
object DemandConfiguration
  extends EqualzAndShowz[DemandConfiguration] {

  implicit def formatDemandConfiguration: Format[DemandConfiguration] =
    Json.format[DemandConfiguration]
}

sealed trait OrganizationId
object OrganizationId
  extends EqualzAndShowz[OrganizationId] {

  object Keys {
    val DpId = "dpId"
    val SpId = "spId"
  }

  def fold[A](
    id: OrganizationId,
    fDpId: DemandPartnerOrganizationId => A,
    fSpId: SupplyPartnerOrganizationId => A): A = id match {
    case dpId: DemandPartnerOrganizationId => fDpId(dpId)
    case spId: SupplyPartnerOrganizationId => fSpId(spId)
  }

  val reads: Reads[OrganizationId] =
      (__ \ Keys.DpId).read[DemandPartnerId].map(dpId) orElse
      (__ \ Keys.SpId).read[SupplyPartnerId].map(spId) orElse
      Reads(js => PlayJsonUtils.jsError(s"OrganizationId is not valid, json: '$js'"))

  val writes: Writes[OrganizationId] = Writes {
    fold(_,
      id => Json.toJson(Map(Keys.DpId -> id.id)),
      id => Json.toJson(Map(Keys.SpId -> id.id)))
  }

  implicit val formats: Format[OrganizationId] = Format(reads, writes)

  def spId(value: SupplyPartnerId): OrganizationId =
    SupplyPartnerOrganizationId(value)
  def dpId(value: DemandPartnerId): OrganizationId =
    DemandPartnerOrganizationId(value)

}

case class DemandPartnerOrganizationId(id: DemandPartnerId)
  extends OrganizationId
case class SupplyPartnerOrganizationId(id: SupplyPartnerId)
  extends OrganizationId

case class Organization(
  id: OrganizationId,
  name: String)
object Organization
  extends EqualzAndShowz[Organization] {

  implicit def formatOrganization: Format[Organization] =
    Json.format[Organization]
}

case class UserDetails(
  username: Username,
  email: EmailAddress,
  fullname: Fullname,
  preferences: UserPreferences,
  organization: Organization)
object UserDetails
  extends EqualzAndShowz[UserDetails] {

  implicit def formatUserDetails: Format[UserDetails] =
    Json.format[UserDetails]
}

case class AuthenticatedUser(
  username: Username,
  email: EmailAddress,
  fullname: Fullname,
  preferences: UserPreferences,
  organization: Organization,
  demandConfig: DemandConfiguration) {

  def credentials(): (DemandPartnerId,List[SupplyPartner]) = {

    (demandPartner -> supplyPartners)
  }

  def demandPartner() : DemandPartnerId = {
    organization.id match {
      case DemandPartnerOrganizationId(s) => s
      case _ => DemandPartnerId("notfound")
    }
  }

  def supplyPartners() : List[SupplyPartner] = {
    demandConfig.supplyPartners
  }

}
object AuthenticatedUser
  extends EqualzAndShowz[AuthenticatedUser] {

  implicit def formatAuthenticatedUser: Format[AuthenticatedUser] =
    Json.format[AuthenticatedUser]
}

case class AuthenticationError(status: Int, code: Int, message: String, developerMessage: String, moreInfo: String, error: String)

object AuthenticationError
  extends EqualzAndShowz[AuthenticationError] {

  object Keys {
    val Status = "status"
    val Code = "code"
    val Message = "message"
    val DeveloperMessage = "developerMessage"
    val MoreInfo = "moreInfo"
    val Error = "error"
  }

  implicit val reads: Reads[AuthenticationError] = {
    ((__ \ Keys.Status).read[Int] ~
      (__ \ Keys.Code).read[Int] ~
      (__ \ Keys.Message).read[String] ~
      (__ \ Keys.DeveloperMessage).read[String] ~
      (__ \ Keys.MoreInfo).read[String] ~
      (__ \ Keys.Error).read[String]
      )(AuthenticationError.apply _)
  }

  def Other: AuthenticationError =
    AuthenticationError(-1, -1, null, null, null, "Other Error")

}

case class UnauthorizedAccess(token: AccessToken)
object UnauthorizedAccess
  extends EqualzAndShowz[UnauthorizedAccess]

object OAuthRejections {
  def rejection(e: AuthenticationError): Rejection =
    AuthenticationRejection(e.message)
}