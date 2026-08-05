/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.auth.PertaxAuthAction
import errors.ErrorResponseStatus.CITIZEN_NOT_FOUND
import errors.{FindRecipientCodedErrorResponse, RecipientDeceasedError, TransferorDeceasedError, UpdateRelationshipError}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.MarriageAllowanceService
import test_utils.{FakePertaxAuthAction, TestData, UnitSpec}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{BadRequestException, NotFoundException, UpstreamErrorResponse}

import scala.concurrent.Future

//TODO All these tests are testing MarriageAllowanceController, surely this should already be tested.
class ErrorSpec extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach {

  val mockMarriageAllowanceService: MarriageAllowanceService = mock[MarriageAllowanceService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MarriageAllowanceService].toInstance(mockMarriageAllowanceService),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  val controller: MarriageAllowanceController = app.injector.instanceOf[MarriageAllowanceController]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMarriageAllowanceService)
  }

  val generatedNino = new Generator().nextNino.nino

  "Checking user record" should {

    // TODO this needs investigating
    "return Recipient not found if there is an error while finding cid for recipient" in {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP2A)
      val transferorNino       = Nino(transferorNinoObject.nino)

      val recipient       = TestData.Recipients.recHasNoAllowanceNoCid
      val recipientNino   = recipient.citizen.nino
      val recipientGender = recipient.gender

      when(mockMarriageAllowanceService.getRecipientRelationship(meq(transferorNino), any())(any(), any()))
        .thenReturn(Future.successful(Left(FindRecipientCodedErrorResponse(1, 1, ""))))

      val testData = s"""{"name":"foo","lastName":"bar", "nino":"$recipientNino", "gender":"$recipientGender"}"""
      val request  = FakeRequest().withBody(Json.parse(testData))

      val result = controller.getRecipientRelationship(transferorNino)(request)
      status(result) shouldBe NOT_FOUND

      val json = Json.parse(contentAsString(result)(defaultTimeout))
      (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:RECIPIENT-NOT-FOUND"
    }

    "when transferor deceased" should {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP6A)
      val transferorNino       = Nino(transferorNinoObject.nino)

      "return transferor deceased BadRequest when recipient is good (has allowance)" in {
        val recipient       = TestData.Recipients.recHasAllowance
        val recipientNino   = recipient.citizen.nino
        val recipientGender = recipient.gender

        when(mockMarriageAllowanceService.getRecipientRelationship(meq(transferorNino), any())(any(), any()))
          .thenReturn(Future.failed(TransferorDeceasedError("Transferor is deceased")))

        val testData = s"""{"name":"rty","lastName":"qwe", "nino":"$recipientNino", "gender":"$recipientGender"}"""
        val request  = FakeRequest().withBody(Json.parse(testData))

        val result = controller.getRecipientRelationship(transferorNino)(request)
        status(result) shouldBe BAD_REQUEST

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:TRANSFERER-DECEASED"
      }

      "return transferor deceased BadRequest when recipient has allowance and space in name" in {

        val recipient       = TestData.Recipients.recHasAllowanceAndSpaceInName
        val recipientNino   = recipient.citizen.nino
        val recipientGender = recipient.gender

        when(mockMarriageAllowanceService.getRecipientRelationship(meq(transferorNino), any())(any(), any()))
          .thenReturn(Future.failed(TransferorDeceasedError("Transferor is deceased")))

        val testData = s"""{"name":"rty","lastName":"qwe abc", "nino":"$recipientNino", "gender":"$recipientGender"}"""
        val request  = FakeRequest().withBody(Json.parse(testData))

        val result = controller.getRecipientRelationship(transferorNino)(request)
        status(result) shouldBe BAD_REQUEST

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:TRANSFERER-DECEASED"
      }

      "return transferor deceased BadRequest when recipient has no allowance" in {

        val recipient       = TestData.Recipients.recHasNoAllowance
        val recipientNino   = recipient.citizen.nino
        val recipientGender = recipient.gender

        when(mockMarriageAllowanceService.getRecipientRelationship(meq(transferorNino), any())(any(), any()))
          .thenReturn(Future.failed(TransferorDeceasedError("Transferor is deceased")))

        val testData = s"""{"name":"fgh","lastName":"asd", "nino":"$recipientNino", "gender":"$recipientGender"}"""
        val request  = FakeRequest().withBody(Json.parse(testData))

        val result = controller.getRecipientRelationship(transferorNino)(request)
        status(result) shouldBe BAD_REQUEST

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:TRANSFERER-DECEASED"
      }

      "return transferor deceased BadRequest when recipient not found" in {

        val testData = s"""{"name":"abc","lastName":"def", "nino":"$generatedNino", "gender":"M"}"""
        val request  = FakeRequest().withBody(Json.parse(testData))

        when(mockMarriageAllowanceService.getRecipientRelationship(meq(Nino(generatedNino)), any())(any(), any()))
          .thenReturn(Future.failed(TransferorDeceasedError("Transferor is deceased")))

        val result = controller.getRecipientRelationship(Nino(generatedNino))(request)
        status(result) shouldBe BAD_REQUEST

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:TRANSFERER-DECEASED"
      }
    }

    // TODO this used to use TestUtility there is no reference to BadRequest in the controller code.
    // This has likely never returned BadRequest
    "return BadRequest if gender is invalid" in {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP2A)
      val transferorNino       = Nino(transferorNinoObject.nino)

      val recipient     = TestData.Recipients.recHasNoAllowance
      val recipientNino = recipient.citizen.nino

      val testData                  = s"""{"name":"fgh","lastName":"asd", "nino":"$recipientNino", "gender":"123"}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result                    = controller.getRecipientRelationship(transferorNino)(request)

      status(result) shouldBe BAD_REQUEST
    }

    "listRelationship" should {
      "return bad request should be handled" in {

        val request = FakeRequest()

        val testData = TestData.Lists.badRequest
        val testNino = Nino(testData.user.nino)

        when(mockMarriageAllowanceService.listRelationship(meq(testNino))(any(), any()))
          .thenReturn(Future.failed(new BadRequestException("Bad Request")))

        val result = controller.listRelationship(testNino)(request)
        status(result) shouldBe BAD_REQUEST

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:BAD-REQUEST"
      }

      "return NotFound should be handled" in {

        val request = FakeRequest()

        val testData = TestData.Lists.citizenNotFound
        val testNino = Nino(testData.user.nino)

        when(mockMarriageAllowanceService.listRelationship(meq(testNino))(any(), any()))
          .thenReturn(Future.failed(new NotFoundException("Not Found")))

        val result = controller.listRelationship(testNino)(request)
        status(result) shouldBe NOT_FOUND

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:CITIZEN-NOT-FOUND"
      }

      "return InternalServerException should be handled" in {

        val request = FakeRequest()

        val testData = TestData.Lists.serverError
        val testNino = Nino(testData.user.nino)

        when(mockMarriageAllowanceService.listRelationship(meq(testNino))(any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("InternalServerError", INTERNAL_SERVER_ERROR)))

        val result = controller.listRelationship(testNino)(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "ERROR:500"
      }

      "return Service unavailable should be handled" in {

        val request = FakeRequest()

        val testData = TestData.Lists.serviceUnavailable
        val testNino = Nino(testData.user.nino)

        when(mockMarriageAllowanceService.listRelationship(meq(testNino))(any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Service Unavailable", SERVICE_UNAVAILABLE)))

        val result = controller.listRelationship(testNino)(request)
        status(result) shouldBe SERVICE_UNAVAILABLE

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "ERROR:503"
      }

      "return BAD_REQUEST should be handled" in {

        val request = FakeRequest()

        val testData = TestData.Lists.serviceUnavailable
        val testNino = Nino(testData.user.nino)

        when(mockMarriageAllowanceService.listRelationship(meq(testNino))(any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Bad Request", BAD_REQUEST)))

        val result = controller.listRelationship(testNino)(request)
        status(result) shouldBe BAD_REQUEST

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:BAD-REQUEST"
      }

      "return Notfound should be handled" in {

        val request = FakeRequest()

        val testData = TestData.Lists.serviceUnavailable
        val testNino = Nino(testData.user.nino)

        when(mockMarriageAllowanceService.listRelationship(meq(testNino))(any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse(CITIZEN_NOT_FOUND, NOT_FOUND)))

        val result = controller.listRelationship(testNino)(request)
        status(result) shouldBe NOT_FOUND

        val json = Json.parse(contentAsString(result)(defaultTimeout))
        (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:CITIZEN-NOT-FOUND"
      }

    }
  }

  "Update Relationship " should {

    "handle Bad request and show transferor is deceased" in {

      val testInput      = TestData.Updates.badRequest
      val recipientCid   = testInput.transferor.cid.cid
      val transferorNino = Nino(testInput.recipient.nino)
      val recipientTs    = testInput.transferor.timestamp
      val transferorTs   = testInput.recipient.timestamp

      when(mockMarriageAllowanceService.updateRelationship(any())(any(), any()))
        .thenReturn(Future.failed(RecipientDeceasedError("Recipient Deceased")))

      val testData                  =
        s"""{"request":{"participant1":{"instanceIdentifier":"$recipientCid","updateTimestamp":"$recipientTs"},"participant2":{"updateTimestamp":"$transferorTs"},"relationship":{"creationTimestamp":"20150531235901","relationshipEndReason":"Cancelled by Transferor","actualEndDate":"20101230"}},"notification":{"full_name":"UNKNOWN","email":"example@example.com","role":"Transferor", "welsh":false, "isRetrospective":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result                    = controller.updateRelationship(transferorNino)(request)
      status(result) shouldBe BAD_REQUEST

      val json = Json.parse(contentAsString(result)(defaultTimeout))
      (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:BAD-REQUEST"
    }

    "handle Update relationship error" in {

      val testInput      = TestData.Updates.citizenNotFound
      val recipientCid   = testInput.transferor.cid.cid
      val transferorNino = Nino(testInput.recipient.nino)
      val recipientTs    = testInput.transferor.timestamp
      val transferorTs   = testInput.recipient.timestamp

      when(mockMarriageAllowanceService.updateRelationship(any())(any(), any()))
        .thenReturn(Future.failed(UpdateRelationshipError("Update Relationship Error")))

      val testData                  =
        s"""{"request":{"participant1":{"instanceIdentifier":"$recipientCid","updateTimestamp":"$recipientTs"},"participant2":{"updateTimestamp":"$transferorTs"},"relationship":{"creationTimestamp":"20150531235901","relationshipEndReason":"Cancelled by Transferor","actualEndDate":"20101230"}},"notification":{"full_name":"UNKNOWN","email":"example@example.com","role":"Transferor", "welsh":false, "isRetrospective":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result                    = controller.updateRelationship(transferorNino)(request)
      status(result) shouldBe BAD_REQUEST

      val json = Json.parse(contentAsString(result)(defaultTimeout))
      (json \ "status" \ "status_code").as[String] shouldBe "TAMC:ERROR:CANNOT-UPDATE-RELATIONSHIP"
    }
  }
}
