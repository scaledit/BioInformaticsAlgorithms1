package com.ntoggle.kubitschek.integration

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.ParameterDirectives
import akka.stream.Materializer
import com.ntoggle.kubitschek.infra.CustomErrorHandlers._
import com.ntoggle.kubitschek.infra.{PlayJsonSupportExt, ValidatedDirectives}
import play.api.libs.json.Json

class MockStormPathService(
  host: String,
  cred: (Username, Password),
  appId: String,
  accountId: String,
  tokenValue: AccessToken,
  refreshValue: AccessToken
  )(implicit materializer: Materializer)
  extends ValidatedDirectives
  with ParameterDirectives {


  val errorResp = s"""
          {
            "status": 200,
            "code": 1234,
            "message": "error message",
            "developerMessage": "developer error message",
            "moreInfo": "more info",
            "error": "the real error"
          }"""

  val tokenResp = s"""
          {
            "access_token":"${tokenValue.value}",
            "refresh_token": "${refreshValue.value}",
            "token_type": "Bearer",
            "expires_in": 3600,
            "stormpath_access_token_href": "$host/v1/accessTokens/r0klomitodnOCuvESIP5"
          }"""

  val fullTokenResp = s"""
         {
           "href":"$host/v1/accessTokens/${tokenValue.value}",
           "createdAt":"2015-09-03T01:57:42.881Z",
           "jwt":"eyJraWQiOiI1QTQwS0c1VUg2TlJZQTVQUk1NTDQzQ1hQIiwiYWxnIjoiSFMyNTYifQ.eyJqdGkiOiIxSEZHdHpHT2M3MWREUDVic0VzSXA1IiwiaWF0IjoxNDQxMjQ1NDYyLCJpc3MiOiJodHRwczovL2FwaS5zdG9ybXBhdGguY29tL3YxL2FwcGxpY2F0aW9ucy81azR3WVRIQ1dlRHhGU2NsM2gxaEpZIiwic3ViIjoiaHR0cHM6Ly9hcGkuc3Rvcm1wYXRoLmNvbS92MS9hY2NvdW50cy9EMDdNZE1IaXRwYk9Edm8yUlI4ZGgiLCJleHAiOjE0NDEyNDkwNjIsInJ0aSI6IjFIRkd0dndKaFdpZWFaemtmejEyRDEifQ.1EFVR4LWEMnhVRAX2DAIQ4Owd1AxRkuYXNA_ZSB0FHA",
           "expandedJwt":{
             "header":{
               "kid":"5A40KG5UH6NRYA5PRMML43CXP",
               "alg":"HS256"
             },
             "claims":{
               "jti":"${tokenValue.value}",
               "iat":1441245462,
               "iss":"$host/v1/applications/$appId",
               "sub":"$host/v1/accounts/$accountId",
               "exp":1441249062,
               "rti":"1HFGtvwJhWieaZzkfz12D1"
             },
             "signature":"1EFVR4LWEMnhVRAX2DAIQ4Owd1AxRkuYXNA_ZSB0FHA"
           },
           "account":{"href":"$host/v1/accounts/$accountId"},
           "application":{"href":"$host/v1/applications/$appId"},
           "tenant":{"href":"$host/v1/tenants/5jYAXVR8QYnaRgVIyLFdMe"}
         }"""

  val accountResp = s"""
         {
           "href":"$host/v1/accounts/$accountId",
           "username":"test",
           "email":"test@ntoggle.com",
           "givenName":"Test",
           "middleName":"",
           "surname":"Test",
           "fullName":"Test Test",
           "status":"ENABLED",
           "createdAt":"2015-08-18T19:33:39.528Z",
           "modifiedAt":"2015-08-18T19:33:39.528Z",
           "emailVerificationToken":null,
           "customData":{
             "href":"$host/v1/accounts/$accountId/customData",
             "createdAt":"2015-08-18T19:33:39.528Z",
             "modifiedAt":"2015-09-03T01:29:13.926Z",
             "organization": {
             "id" : {"dpId" : "2e83b2ea-0e9e-4d4d-a330-a1d0b9065270"},
             "name" : "name"
             },
             "supplyPartners" : [
             {
             "id": "9a726b7f-e36a-441f-b83a-2581c0edfcd3",
             "name": "EXCH1"
             }
             ],
             "configurations":[
                {
                  "demandPartner": {
                    "id": "3f214842-6b16-483f-b49f-d152e01c6e33",
                    "name": "DSP1"
                  },
                  "supplyPartner": {
                    "id": "9a726b7f-e36a-441f-b83a-2581c0edfcd3",
                    "name": "EXCH1"
                  },
                  "supplyType": "mobile"
                }
             ]
           },
           "providerData":{"href":"$host/v1/accounts/$accountId/providerData"},
           "directory":{"href":"$host/v1/directories/5kEHtaZjshM03X6qgwjTfM"},
           "tenant":{"href":"$host/v1/tenants/5jYAXVR8QYnaRgVIyLFdMe"},
           "groups":{"href":"$host/v1/accounts/$accountId/groups"},
           "applications":{"href":"$host/v1/accounts/$accountId/applications"},
           "groupMemberships":{"href":"$host/v1/accounts/$accountId/groupMemberships"},
           "apiKeys":{"href":"$host/v1/accounts/$accountId/apiKeys"},
           "accessTokens":{"href":"$host/v1/accounts/$accountId/accessTokens"},
           "refreshTokens":{"href":"$host/v1/accounts/$accountId/refreshTokens"}
         }"""

  val routes =
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {

        import PlayJsonSupportExt._

        logRequestResult("kubitschek-http-service") {
          pathPrefix("v1") {
            pathPrefix("applications" / appId) {
              pathPrefix("oauth" / "token") {
                post {
                  formFields('username ?, 'password ?, 'grant_type ?, 'refresh_token ?) { (username, password, grantType, refresh) =>
                    grantType match {
                      case Some("password") =>
                        (username, password) match {
                          case (Some (cred._1.value), Some (cred._2.value) ) => complete (Json.parse (tokenResp) )
                          case _ => complete (StatusCodes.BadRequest -> Json.parse (errorResp) )
                        }
                      case Some("refresh_token") =>
                        refresh match {
                          case Some(refreshValue.value) => complete (Json.parse (tokenResp) )
                          case _ => complete (StatusCodes.BadRequest -> Json.parse (errorResp) )
                        }
                      case _ => complete (StatusCodes.BadRequest -> Json.parse (errorResp) )
                    }
                  }
                }
              } ~
                pathPrefix("authTokens") {
                  (get & path(pathValue[String])) {
                    case tokenValue.value => respondWithHeader(RawHeader("Location", s"$host/v1/accessTokens/${tokenValue.value}")) {
                      complete(StatusCodes.Found -> null)
                    }
                    case _ => complete(StatusCodes.BadRequest -> Json.parse (errorResp))
                  }
                }
            } ~
              pathPrefix("accessTokens") {
                (get & path(pathValue[String])) {
                  case tokenValue.value => complete(Json.parse(fullTokenResp))
                  case _ => complete(StatusCodes.BadRequest -> Json.parse (errorResp))
                }
              } ~
              pathPrefix("accounts") {
                (get & path(pathValue[String]) & parameters("expand".as[String])) { (account, expand) =>
                  expand match {
                    case "customData" => complete (Json.parse (accountResp) )
                    case _ => complete(StatusCodes.BadRequest -> Json.parse (errorResp))
                  }
                }
              }
          }
        }
      }
    }
}

