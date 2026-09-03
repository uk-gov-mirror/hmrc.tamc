/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.*
import config.ApplicationConfig
import errors.ErrorResponseStatus
import errors.ErrorResponseStatus.*
import models.*
import models.emailAddress.EmailAddress
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNull, Json}
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers.{status as getStatus, *}
import play.api.test.{FakeHeaders, FakeRequest}
import test_utils.FileHelper.*
import test_utils.{IntegrationSpec, MarriageAllowanceFixtures}
import uk.gov.hmrc.domain.{Nino, NinoGenerator}
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.concurrent.ExecutionException

class MarriageAllowanceControllerISpec extends IntegrationSpec with MarriageAllowanceFixtures {

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port"                   -> wireMockServer.port(),
      "microservice.services.marriage-allowance-des.host" -> "127.0.0.1",
      "microservice.services.marriage-allowance-des.port" -> wireMockServer.port(),
      "microservice.services.pertax.host"                 -> "127.0.0.1",
      "microservice.services.pertax.port"                 -> wireMockServer.port()
    )
    .build()

  override def beforeEach(): Unit  = {
    super.beforeEach()
    wireMockServer.stubFor(
      post(urlEqualTo("/auth/authorise")).willReturn(ok(findCitizenResponse(123456789).toString()))
    )
    wireMockServer.stubFor(post(urlEqualTo("/pertax/authorise")).willReturn(ok(successPertaxAuthResponse.toString())))
  }
  val nino: Nino                   = NinoGenerator().nextNino
  val userRecordCid                = 123456789
  val transferorRecordCid          = 987654321
  val appConfig: ApplicationConfig = baseApplicationBuilder.injector().instanceOf[ApplicationConfig]
  val currentYear: Int             = appConfig.currentTaxYear()

  "getRecipientRelationship" should {
    val findRecipientRequest: FindRecipientRequest =
      FindRecipientRequest("", "", Gender("M"), nino, Some(LocalDate.parse("2010-01-01")))
    val json                                       = AnyContentAsJson(Json.toJson(findRecipientRequest))

    val request = FakeRequest(
      POST,
      s"/paye/$nino/get-recipient-relationship",
      FakeHeaders(Seq("Authorization" -> "Bearer bearer-token")),
      json
    )

    "return a success when getting a successful response from the downstream" in {

      val upratedParticipantStartDate: Timestamp = listRelationshipResponse.toString
        .replaceFirst("""participant1StartDate":"20210406""", s"""participant1StartDate":"${currentYear + 1}0406""")
        .replaceFirst("""participant2StartDate":"20210406""", s"""participant2StartDate":"${currentYear + 1}0406""")

      wireMockServer.stubFor(
        post(urlEqualTo(s"/marriage-allowance/citizen/$nino/check"))
          .willReturn(ok(getRecipientRelationshipResponse(userRecordCid).toString))
      )
      wireMockServer.stubFor(
        get(urlEqualTo(s"/marriage-allowance/citizen/$nino"))
          .willReturn(ok(findCitizenResponse(transferorRecordCid).toString))
      )
      wireMockServer.stubFor(
        get(urlEqualTo(s"/marriage-allowance/citizen/$userRecordCid/relationships?includeHistoric=true"))
          .willReturn(ok(upratedParticipantStartDate))
      )
      wireMockServer.stubFor(
        get(urlEqualTo(s"/marriage-allowance/citizen/$transferorRecordCid/relationships?includeHistoric=true"))
          .willReturn(ok(upratedParticipantStartDate))
      )

      val result   = route(fakeApplication(), request)
      val expected = Json.toJson(
        GetRelationshipResponse(
          Some(UserRecord(userRecordCid, "20200116155359011123")),
          Some(
            List(
              TaxYear(currentYear, Some(true)),
              TaxYear(currentYear - 1),
              TaxYear(currentYear - 2),
              TaxYear(currentYear - 3),
              TaxYear(currentYear - 4)
            )
          ),
          ResponseStatus("OK")
        )
      )

      result.map(getStatus)     shouldBe Some(OK)
      result.map(contentAsJson) shouldBe Some(expected)
    }

    List(2016, 2017, 2018, 2039, 2040, 2061, 2).foreach { reasonCode =>
      s"return an error when a reason code of $reasonCode is returned" in {

        wireMockServer.stubFor(
          post(urlEqualTo(s"/marriage-allowance/citizen/$nino/check")).willReturn(
            ok(getRecipientRelationshipResponse(userRecordCid, reasonCode = reasonCode, returnCode = -1011).toString())
          )
        )

        val result   = route(fakeApplication(), request)
        val expected = Json.toJson(GetRelationshipResponse(status = ResponseStatus(status_code = RECIPIENT_NOT_FOUND)))

        result.map(getStatus)     shouldBe Some(NOT_FOUND)
        result.map(contentAsJson) shouldBe Some(expected)
      }
    }

    "return a TransferorDeceasedError when the transferor is shown as deceased" in {

      wireMockServer.stubFor(
        post(urlEqualTo(s"/marriage-allowance/citizen/$nino/check"))
          .willReturn(ok(getRecipientRelationshipResponse(userRecordCid).toString()))
      )
      wireMockServer.stubFor(
        get(urlEqualTo(s"/marriage-allowance/citizen/$nino"))
          .willReturn(ok(findCitizenResponse(transferorRecordCid, "Y").toString()))
      )

      val result   = route(fakeApplication(), request)
      val expected = Json.toJson(GetRelationshipResponse(status = ResponseStatus(status_code = TRANSFERER_DECEASED)))

      result.map(getStatus)     shouldBe Some(play.api.http.Status.BAD_REQUEST)
      result.map(contentAsJson) shouldBe Some(expected)
    }

    "return a RECIPIENT_NOT_FOUND error when a serviceError occurs" in {

      wireMockServer.stubFor(
        post(urlEqualTo(s"/marriage-allowance/citizen/$nino/check"))
          .willReturn(ok(getRecipientRelationshipResponse(userRecordCid).toString()))
      )
      wireMockServer.stubFor(
        get(urlEqualTo(s"/marriage-allowance/citizen/$nino"))
          .willReturn(ok(findCitizenResponse(transferorRecordCid, reasonCode = 2, returnCode = 2).toString()))
      )

      val result   = route(fakeApplication(), request)
      val expected = Json.toJson(GetRelationshipResponse(status = ResponseStatus(status_code = RECIPIENT_NOT_FOUND)))

      result.map(getStatus)     shouldBe Some(NOT_FOUND)
      result.map(contentAsJson) shouldBe Some(expected)
    }

    "return a validation error when downstream returns an invalid json" in {

      wireMockServer.stubFor(
        post(urlEqualTo(s"/marriage-allowance/citizen/$nino/check"))
          .willReturn(ok(getRecipientRelationshipResponse(userRecordCid).toString()))
      )
      wireMockServer.stubFor(
        get(urlEqualTo(s"/marriage-allowance/citizen/$nino")).willReturn(ok(Json.parse("""{}""").toString()))
      )

      val result   = route(fakeApplication(), request)
      val expected = Json.toJson(GetRelationshipResponse(status = ResponseStatus(status_code = OTHER_ERROR)))

      result.map(getStatus)     shouldBe Some(INTERNAL_SERVER_ERROR)
      result.map(contentAsJson) shouldBe Some(expected)
    }

    "return a server error when thrown from downstream" in {

      wireMockServer.stubFor(
        post(urlEqualTo(s"/marriage-allowance/citizen/$nino/check"))
          .willReturn(ok(getRecipientRelationshipResponse(userRecordCid).toString()))
      )
      wireMockServer.stubFor(
        get(urlEqualTo(s"/marriage-allowance/citizen/$nino"))
          .willReturn(ok(findCitizenResponse(transferorRecordCid).toString()))
      )
      wireMockServer.stubFor(
        get(urlEqualTo(s"/marriage-allowance/citizen/$userRecordCid/relationships?includeHistoric=true"))
          .willReturn(serverError())
      )

      val result   = route(fakeApplication(), request)
      val expected = Json.toJson(GetRelationshipResponse(status = ResponseStatus(status_code = OTHER_ERROR)))

      result.map(getStatus)     shouldBe Some(INTERNAL_SERVER_ERROR)
      result.map(contentAsJson) shouldBe Some(expected)
    }

    List(
      notFound()                                -> "404",
      serverError()                             -> "500",
      serviceUnavailable()                      -> "503",
      badRequest()                              -> "400",
      aResponse().withStatus(GATEWAY_TIMEOUT)   -> "499",
      aResponse().withStatus(BAD_GATEWAY)       -> "502",
      aResponse().withStatus(TOO_MANY_REQUESTS) -> "429"
    ).foreach { case (errorResponse, errorCode) =>
      s"return other error for $errorCode" in {

        wireMockServer.stubFor(post(urlEqualTo(s"/marriage-allowance/citizen/$nino/check")).willReturn(errorResponse))

        val result   = route(fakeApplication(), request)
        val expected = Json.toJson(GetRelationshipResponse(status = ResponseStatus(status_code = RECIPIENT_NOT_FOUND)))

        result.map(getStatus)     shouldBe Some(NOT_FOUND)
        result.map(contentAsJson) shouldBe Some(expected)
      }
    }
  }

  "createMultiYearRelationship" should {
    val journey = "newJourney"

    val multiYearCreateRelationshipRequest: MultiYearCreateRelationshipRequest = MultiYearCreateRelationshipRequest(
      transferor_cid = 1111.asInstanceOf[Cid],
      transferor_timestamp = "2222",
      recipient_cid = 3333.asInstanceOf[Cid],
      recipient_timestamp = "4444",
      taxYears = List(2015, 2016, LocalDate.now().getYear)
    )

    val createRelationshipNotificationRequest: CreateRelationshipNotificationRequest =
      CreateRelationshipNotificationRequest(
        full_name = "bob",
        email = EmailAddress("bob@yahoo.com"),
        welsh = false
      )

    val multiYearCreateRelationshipRequestHolder: MultiYearCreateRelationshipRequestHolder =
      MultiYearCreateRelationshipRequestHolder(
        multiYearCreateRelationshipRequest,
        createRelationshipNotificationRequest
      )

    val json = Json.toJson(multiYearCreateRelationshipRequestHolder)

    val request = FakeRequest(
      method = PUT,
      uri = s"/paye/$nino/create-multi-year-relationship/$journey",
      headers = FakeHeaders(
        Seq(
          "Authorization" -> "Bearer bearer-token"
        )
      ),
      body = json
    )

    "return a success when successfully creating a multi year relationship" in {
      wireMockServer.stubFor(
        post(
          urlEqualTo(
            s"/marriage-allowance/02.00.00/citizen/${multiYearCreateRelationshipRequestHolder.request.recipient_cid}/relationship/retrospective"
          )
        ).willReturn(ok(createMultiYearRelationshipResponse.toString()))
      )
      wireMockServer.stubFor(
        post(
          urlEqualTo(
            s"/marriage-allowance/02.00.00/citizen/${multiYearCreateRelationshipRequestHolder.request.recipient_cid}/relationship/active"
          )
        ).willReturn(ok(createMultiYearRelationshipResponse.toString()))
      )

      val result = route(fakeApplication(), request)

      result.map(getStatus) shouldBe Some(OK)
    }

    "return an error when downstream returns LTM000503" in {
      wireMockServer.stubFor(
        post(
          urlEqualTo(
            s"/marriage-allowance/02.00.00/citizen/${multiYearCreateRelationshipRequestHolder.request.recipient_cid}/relationship/retrospective"
          )
        ).willReturn(aResponse().withStatus(CONFLICT).withBody(LTM000503Error.toString()))
      )

      val result   = route(fakeApplication(), request)
      val expected =
        Json.toJson(CreateRelationshipResponse(status = ResponseStatus(status_code = RELATION_MIGHT_BE_CREATED)))
      result.map(getStatus)     shouldBe Some(CONFLICT)
      result.map(contentAsJson) shouldBe Some(expected)
    }

    "return an error when unable to update as participant" in {
      wireMockServer.stubFor(
        post(
          urlEqualTo(
            s"/marriage-allowance/02.00.00/citizen/${multiYearCreateRelationshipRequestHolder.request.recipient_cid}/relationship/retrospective"
          )
        ).willReturn(aResponse().withStatus(CONFLICT).withBody(unableToUpdateError.toString()))
      )

      val result   = route(fakeApplication(), request)
      val expected =
        Json.toJson(CreateRelationshipResponse(status = ResponseStatus(status_code = RELATION_MIGHT_BE_CREATED)))
      result.map(getStatus)     shouldBe Some(CONFLICT)
      result.map(contentAsJson) shouldBe Some(expected)
    }

    "throw an error when any other error is thrown" in {
      wireMockServer.stubFor(
        post(
          urlEqualTo(
            s"/marriage-allowance/02.00.00/citizen/${multiYearCreateRelationshipRequestHolder.request.recipient_cid}/relationship/retrospective"
          )
        ).willReturn(badRequest().withBody(createMultiYearError.toString()))
      )

      val result = route(fakeApplication(), request)

      assertThrows[UpstreamErrorResponse] {
        result.map(await(_))
      }
    }
  }

  "listRelationship" should {
    val request = FakeRequest(
      GET,
      s"/paye/$nino/list-relationship",
      FakeHeaders(Seq("Authorization" -> "Bearer bearer-token")),
      JsNull
    )

    "return a success when getting a successful response from the downstream" in {
      wireMockServer.stubFor(
        get(urlEqualTo(s"/marriage-allowance/citizen/$nino"))
          .willReturn(ok(findCitizenResponse(userRecordCid).toString()))
      )
      wireMockServer.stubFor(
        get(urlEqualTo(s"/marriage-allowance/citizen/$userRecordCid/relationships?includeHistoric=true"))
          .willReturn(ok(listRelationshipResponse.toString()))
      )

      val result = route(fakeApplication(), request)

      result.map(getStatus) shouldBe Some(OK)
    }

    "returns TransferorDeceasedError when the citizen is listed as deceased" in {
      wireMockServer.stubFor(
        get(urlEqualTo(s"/marriage-allowance/citizen/$nino"))
          .willReturn(ok(findCitizenResponse(userRecordCid, "Y").toString()))
      )

      val result   = route(fakeApplication(), request)
      val expected =
        Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = TRANSFEROR_NOT_FOUND)))

      result.map(getStatus)     shouldBe Some(NOT_FOUND)
      result.map(contentAsJson) shouldBe Some(expected)
    }

    "returns a Service Error when an error in the service is thrown" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/marriage-allowance/citizen/$nino"))
          .willReturn(ok(findCitizenResponse(userRecordCid, reasonCode = 2, returnCode = 2).toString()))
      )

      val result   = route(fakeApplication(), request)
      val expected =
        Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = TRANSFEROR_NOT_FOUND)))

      result.map(getStatus)     shouldBe Some(NOT_FOUND)
      result.map(contentAsJson) shouldBe Some(expected)
    }

    "returns any other error when a non captured error is thrown" in {
      wireMockServer.stubFor(
        get(urlEqualTo(s"/marriage-allowance/citizen/$nino")).willReturn(aResponse().withStatus(UNAUTHORIZED))
      )

      val result = route(fakeApplication(), request)

      assertThrows[UpstreamErrorResponse] {
        result.map(await(_))
      }
    }

    "return service error code" when {
      List(
        notFound()           -> CITIZEN_NOT_FOUND                      -> "404",
        badRequest()         -> ErrorResponseStatus.BAD_REQUEST        -> "400",
        serverError()        -> SERVER_ERROR                           -> "500",
        serviceUnavailable() -> ErrorResponseStatus.SERVICE_UNAVILABLE -> "503"
      ).foreach { case ((errorResponse, errorCode), httpErrorCode) =>
        s"citizen endpoint returns $httpErrorCode" in {

          wireMockServer.stubFor(
            get(urlEqualTo(s"/marriage-allowance/citizen/$nino"))
              .willReturn(errorResponse)
          )

          val result   = route(fakeApplication(), request)
          val expected = Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = errorCode)))

          result.map(getStatus)     shouldBe Some(httpErrorCode.toInt)
          result.map(contentAsJson) shouldBe Some(expected)
        }

        s"listRelationship endpoint returns $httpErrorCode" in {

          wireMockServer.stubFor(
            get(urlEqualTo(s"/marriage-allowance/citizen/$nino"))
              .willReturn(ok(loadFile("./it/test/resources/citizenRecord.json")))
          )

          wireMockServer.stubFor(
            get(urlEqualTo(s"/marriage-allowance/citizen/123456/relationships?includeHistoric=true"))
              .willReturn(errorResponse)
          )

          val result   = route(fakeApplication(), request)
          val expected = Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = errorCode)))

          result.map(getStatus)     shouldBe Some(httpErrorCode.toInt)
          result.map(contentAsJson) shouldBe Some(expected)
        }
      }
    }
  }

  "updateRelationship" should {
    val REASON_CANCEL  = "Cancelled by Transferor"
    val REASON_REJECT  = "Rejected by Recipient"
    val REASON_DIVORCE = "Divorce/Separation"

    val ROLE_TRANSFEROR = "Transferor"
    val ROLE_RECIPIENT  = "Recipient"

    val DATE_FORMAT      = "yyyyMMdd"
    val sdf              = new SimpleDateFormat(DATE_FORMAT)
    val date             = Calendar.getInstance()
    val currentDate      = sdf.format(date.getTime)
    date.add(Calendar.YEAR, -1)
    val previousYearDate = sdf.format(date.getTime)

    List(
      (REASON_CANCEL, ROLE_RECIPIENT, false, currentDate),
      (REASON_REJECT, ROLE_RECIPIENT, true, currentDate),
      (REASON_REJECT, ROLE_RECIPIENT, false, currentDate),
      (REASON_DIVORCE, ROLE_TRANSFEROR, false, currentDate),
      (REASON_DIVORCE, ROLE_TRANSFEROR, false, previousYearDate),
      (REASON_DIVORCE, ROLE_TRANSFEROR, false, "20100101"),
      (REASON_DIVORCE, ROLE_RECIPIENT, false, currentDate),
      (REASON_DIVORCE, ROLE_RECIPIENT, false, "20100101")
    ).foreach { case (reason, role, retrospective, actualEndDate) =>
      s"return a success when the reason is $reason, the role is $role, retrospective is $retrospective and the actualEndDate is $actualEndDate" in {

        wireMockServer.stubFor(
          put(urlEqualTo(s"/marriage-allowance/citizen/123456789/relationship"))
            .willReturn(ok(updateAllowanceRelationshipResponse.toString()))
        )

        val updateRelationshipRequest = DesUpdateRelationshipRequest(
          DesRecipientInformation("123456789", "2222"),
          DesTransferorInformation("4444"),
          DesRelationshipInformation("20101230", reason, actualEndDate)
        )

        val updateRelationshipNotificationRequest = UpdateRelationshipNotificationRequest(
          "John",
          EmailAddress("bob@yahoo.com"),
          role,
          welsh = false,
          isRetrospective = retrospective
        )

        val updateRelationshipRequestHolder =
          UpdateRelationshipRequestHolder(updateRelationshipRequest, updateRelationshipNotificationRequest)

        val json = Json.toJson(updateRelationshipRequestHolder)

        val request = FakeRequest(
          PUT,
          s"/paye/$nino/update-relationship",
          FakeHeaders(Seq("Authorization" -> "Bearer bearer-token")),
          json
        )

        val result   = route(fakeApplication(), request)
        val expected = Json.toJson(UpdateRelationshipResponse(status = ResponseStatus(status_code = "OK")))

        result.map(getStatus)     shouldBe Some(OK)
        result.map(contentAsJson) shouldBe Some(expected)
      }
    }

    "return a RecipientDeceasedError when the recipient is shown to be deceased" in {
      wireMockServer.stubFor(
        put(urlEqualTo(s"/marriage-allowance/citizen/123456789/relationship")).willReturn(badRequest())
      )

      val updateRelationshipRequest = DesUpdateRelationshipRequest(
        DesRecipientInformation("123456789", "2222"),
        DesTransferorInformation("4444"),
        DesRelationshipInformation("20101230", "Divorce/Separation", "20101230")
      )

      val updateRelationshipNotificationRequest = UpdateRelationshipNotificationRequest(
        "John",
        EmailAddress("bob@yahoo.com"),
        "Recipient",
        welsh = false,
        isRetrospective = false
      )

      val updateRelationshipRequestHolder =
        UpdateRelationshipRequestHolder(updateRelationshipRequest, updateRelationshipNotificationRequest)

      val json = Json.toJson(updateRelationshipRequestHolder)

      val request = FakeRequest(
        PUT,
        s"/paye/$nino/update-relationship",
        FakeHeaders(Seq("Authorization" -> "Bearer bearer-token")),
        json
      )

      val result   = route(fakeApplication(), request)
      val expected =
        Json.toJson(UpdateRelationshipResponse(status = ResponseStatus(status_code = ErrorResponseStatus.BAD_REQUEST)))

      result.map(getStatus)     shouldBe Some(play.api.http.Status.BAD_REQUEST)
      result.map(contentAsJson) shouldBe Some(expected)
    }

    "return a UpdateRelationshipError when an unexpected error occurs while updating the relationship" in {
      wireMockServer.stubFor(
        put(urlEqualTo(s"/marriage-allowance/citizen/123456789/relationship")).willReturn(serverError())
      )

      val updateRelationshipRequest = DesUpdateRelationshipRequest(
        DesRecipientInformation("123456789", "2222"),
        DesTransferorInformation("4444"),
        DesRelationshipInformation("20101230", "Divorce/Separation", "20101230")
      )

      val updateRelationshipNotificationRequest = UpdateRelationshipNotificationRequest(
        "John",
        EmailAddress("bob@yahoo.com"),
        "Recipient",
        welsh = true,
        isRetrospective = false
      )

      val updateRelationshipRequestHolder =
        UpdateRelationshipRequestHolder(updateRelationshipRequest, updateRelationshipNotificationRequest)

      val json = Json.toJson(updateRelationshipRequestHolder)

      val request = FakeRequest(
        PUT,
        s"/paye/$nino/update-relationship",
        FakeHeaders(Seq("Authorization" -> "Bearer bearer-token")),
        json
      )

      val result   = route(fakeApplication(), request)
      val expected =
        Json.toJson(UpdateRelationshipResponse(status = ResponseStatus(status_code = CANNOT_UPDATE_RELATIONSHIP)))

      result.map(getStatus)     shouldBe Some(play.api.http.Status.BAD_REQUEST)
      result.map(contentAsJson) shouldBe Some(expected)
    }

    "throw an error whenever any other error occurs" in {

      wireMockServer.stubFor(
        put(urlEqualTo(s"/marriage-allowance/citizen/123456789/relationship"))
          .willReturn(ok(updateAllowanceRelationshipResponse.toString()))
      )

      val updateRelationshipRequest = DesUpdateRelationshipRequest(
        DesRecipientInformation("123456789", "2222"),
        DesTransferorInformation("4444"),
        DesRelationshipInformation("20101230", "Invalid", "20101230")
      )

      val updateRelationshipNotificationRequest = UpdateRelationshipNotificationRequest(
        "John",
        EmailAddress("bob@yahoo.com"),
        "Invalid",
        welsh = false,
        isRetrospective = false
      )

      val updateRelationshipRequestHolder =
        UpdateRelationshipRequestHolder(updateRelationshipRequest, updateRelationshipNotificationRequest)

      val json = Json.toJson(updateRelationshipRequestHolder)

      val request = FakeRequest(
        PUT,
        s"/paye/$nino/update-relationship",
        FakeHeaders(Seq("Authorization" -> "Bearer bearer-token")),
        json
      )

      val result = route(fakeApplication(), request)

      assertThrows[ExecutionException] {
        result.map(await(_))
      }
    }
  }
}
