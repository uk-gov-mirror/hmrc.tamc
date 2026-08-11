/*
 * Copyright 2026 HM Revenue & Customs
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
import errors.*
import errors.ErrorResponseStatus.*
import models.*
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks.*
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, contentAsJson, contentAsString, defaultAwaitTimeout}
import play.api.test.{FakeHeaders, FakeRequest}
import services.MarriageAllowanceService
import test_utils.*
import uk.gov.hmrc.domain.{Nino, NinoGenerator}
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.Future

class MarriageAllowanceControllerSpec extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach {

  val mockMarriageAllowanceService: MarriageAllowanceService = mock[MarriageAllowanceService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MarriageAllowanceService].toInstance(mockMarriageAllowanceService),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  lazy val controller: MarriageAllowanceController = app.injector.instanceOf[MarriageAllowanceController]

  trait Setup {
    val generatedNino: Nino                        = new NinoGenerator().nextNino
    val findRecipientRequest: FindRecipientRequest =
      FindRecipientRequest(name = "testName", lastName = "lastName", gender = Gender("M"), generatedNino)
    val json: JsValue                              = Json.toJson(findRecipientRequest)
    val fakeRequest: FakeRequest[JsValue]          = FakeRequest("POST", "/", FakeHeaders(), Json.toJson(json))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMarriageAllowanceService)
  }

  "Marriage Allowance Controller" should {
    "return OK when a valid UserRecord and TaxYearModel are received" in new Setup {
      val userRecord: UserRecord = UserRecord(cid = 123456789, timestamp = "20200116155359011123")
      val taxYearList            = List(TaxYear(2019))

      when(
        mockMarriageAllowanceService.getRecipientRelationship(
          ArgumentMatchers.eq(generatedNino),
          ArgumentMatchers.eq(findRecipientRequest)
        )(ArgumentMatchers.any(), ArgumentMatchers.any())
      ).thenReturn(Future.successful(Right((userRecord, taxYearList))))

      val result = controller.getRecipientRelationship(generatedNino)(fakeRequest)

      val expectedResponse = Json.toJson(
        GetRelationshipResponse(
          user_record = Some(userRecord),
          availableYears = Some(taxYearList),
          status = ResponseStatus(status_code = "OK")
        )
      )

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(expectedResponse)
    }
  }

  "return a RecipientNotFound error after receiving a DataRetrievalError" in new Setup {

    val records =
      Table(
        "DataRetrivalError",
        BadRequestError,
        TooManyRequestsError,
        ServerError,
        ServiceUnavailableError,
        TimeOutError,
        BadGatewayError,
        UnhandledStatusError,
        ResponseValidationError
      )

    forAll(records) { retrievalError =>

      when(
        mockMarriageAllowanceService.getRecipientRelationship(
          ArgumentMatchers.eq(generatedNino),
          ArgumentMatchers.eq(findRecipientRequest)
        )(ArgumentMatchers.any(), ArgumentMatchers.any())
      ).thenReturn(Future.successful(Left(retrievalError)))

      val result = controller.getRecipientRelationship(generatedNino)(fakeRequest)

      val expectedResponse = GetRelationshipResponse(status = ResponseStatus(status_code = RECIPIENT_NOT_FOUND))

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(expectedResponse)
    }

  }

  "return a RecipientNotFound error after receiving a FindRecipientError" in new Setup {

    val records =
      Table(
        "DataRetrivalError",
        BadRequestError,
        TooManyRequestsError,
        ServerError,
        ServiceUnavailableError,
        TimeOutError,
        BadGatewayError,
        UnhandledStatusError,
        ResponseValidationError
      )

    forAll(records) { retrievalError =>

      when(
        mockMarriageAllowanceService.getRecipientRelationship(
          ArgumentMatchers.eq(generatedNino),
          ArgumentMatchers.eq(findRecipientRequest)
        )(ArgumentMatchers.any(), ArgumentMatchers.any())
      )
        .thenReturn(Future.failed(FindRecipientError(1, 1)))

      val result = controller.getRecipientRelationship(generatedNino)(fakeRequest)

      val expectedResponse = GetRelationshipResponse(status = ResponseStatus(status_code = RECIPIENT_NOT_FOUND))

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(expectedResponse)
    }
  }

  "return an InternalError error after throwing BadRequestException" in new Setup {
    when(
      mockMarriageAllowanceService.getRecipientRelationship(
        ArgumentMatchers.eq(generatedNino),
        ArgumentMatchers.eq(findRecipientRequest)
      )(ArgumentMatchers.any(), ArgumentMatchers.any())
    )
      .thenReturn(Future.failed(new BadRequestException("Exception for test")))

    val result = controller.getRecipientRelationship(generatedNino)(fakeRequest)

    status(result) shouldBe INTERNAL_SERVER_ERROR
  }

  "Calling hasMarriageAllowance for Recipient" should {

    "return OK if cid is found" in {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP2A)
      val transferorNino       = Nino(transferorNinoObject.nino)

      val recipient       = TestData.Recipients.recHasAllowance
      val recipientNino   = recipient.citizen.nino
      val recipientCid    = recipient.citizen.cid.cid
      val recipientGender = recipient.gender
      val userRecord      = UserRecord(recipientCid, recipient.citizen.timestamp)

      when(mockMarriageAllowanceService.getRecipientRelationship(any(), any())(any(), any()))
        .thenReturn(Future.successful(Right((userRecord, List(TaxYear(2019, Some(true)))))))

      val testData                  =
        s"""{"name":"rty","lastName":"qwe", "nino":"$recipientNino", "gender":"$recipientGender", "dateOfMarriage":"01/01/2015"}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result                    = controller.getRecipientRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      (json \ "user_record" \ "has_allowance").asOpt[Boolean] shouldBe None
      (json \ "status" \ "status_code").as[String]            shouldBe "OK"
    }

    "return OK if cid is found and allowance relationship exists and surname has space in between" in {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP2A)
      val transferorNino       = Nino(transferorNinoObject.nino)

      val recipient       = TestData.Recipients.recHasAllowance
      val recipientNino   = recipient.citizen.nino
      val recipientCid    = recipient.citizen.cid.cid
      val recipientGender = recipient.gender
      val userRecord      = UserRecord(recipientCid, recipient.citizen.timestamp)

      when(mockMarriageAllowanceService.getRecipientRelationship(any(), any())(any(), any()))
        .thenReturn(Future.successful(Right((userRecord, List(TaxYear(2019, Some(true)))))))

      val testData                  =
        s"""{"name":"rty","lastName":"qwe abc", "nino":"$recipientNino", "gender":"$recipientGender", "dateOfMarriage":"01/01/2015"}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result                    = controller.getRecipientRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      (json \ "user_record" \ "has_allowance").asOpt[Boolean] shouldBe None
      (json \ "status" \ "status_code").as[String]            shouldBe "OK"
    }

    "return false if cid is found and allowance relationship does not exist" in {

      val transferorNinoObject = TestData.mappedNino2FindCitizen(TestData.Ninos.ninoP2A)
      val transferorNino       = Nino(transferorNinoObject.nino)

      val recipient       = TestData.Recipients.recHasNoAllowance
      val recipientNino   = recipient.citizen.nino
      val recipientCid    = recipient.citizen.cid.cid
      val recipientGender = recipient.gender
      val userRecord      = UserRecord(recipientCid, recipient.citizen.timestamp)

      when(mockMarriageAllowanceService.getRecipientRelationship(any(), any())(any(), any()))
        .thenReturn(Future.successful(Right((userRecord, List(TaxYear(2019, Some(true)))))))

      val testData                  =
        s"""{"name":"fgh","lastName":"asd", "nino":"$recipientNino", "gender":"$recipientGender", "dateOfMarriage":"01/01/2015"}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))
      val result                    = controller.getRecipientRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json = Json.parse(contentAsString(result))
      (json \ "user_record" \ "has_allowance").asOpt[Boolean] shouldBe None
      (json \ "status" \ "status_code").as[String]            shouldBe "OK"
    }
  }

  "Calling list relationship for logged in person" should {

    "check if list contains one active and one historic relationship" in {
      val request = FakeRequest()

      val testData       = TestData.Lists.oneActiveOneHistoric
      val testNino       = Nino(testData.user.nino)
      val testCid        = testData.user.cid.cid
      val testTs: String = testData.user.timestamp

      val participiant0            = testData.counterparties(0)
      val participiant0Cid: String = participiant0.partner.cid.cid.toString
      val participiant0Ts: String  = participiant0.partner.timestamp

      val participiant1            = testData.counterparties(1)
      val participiant1Cid: String = participiant1.partner.cid.cid.toString
      val participiant1Ts: String  = participiant1.partner.timestamp
      val userRecord1              = UserRecord(testCid, testTs)
      val relRecordP0              =
        RelationshipRecord("Recipient", "20150531235901", "20011230", None, None, participiant0Cid, participiant0Ts)
      val relRecordP1              = RelationshipRecord(
        "Recipient",
        "20150531235901",
        "20011230",
        Some(RelationshipEndReason.Death),
        Some("20101230"),
        participiant1Cid,
        participiant1Ts
      )

      when(mockMarriageAllowanceService.listRelationship(any())(any(), any()))
        .thenReturn(Future.successful(RelationshipRecordWrapper(Seq(relRecordP0, relRecordP1), Some(userRecord1))))

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json                            = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[TestRelationshipRecordStatusWrapper]

      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
      val list = relationshipRecordStatusWrapper.relationship_record.relationshipRecordList

      list.size shouldBe 2
      var expectedOutputMap = Map(
        "participant"                        -> "Recipient",
        "creationTimestamp"                  -> "20150531235901",
        "participant1StartDate"              -> "20011230",
        "relationshipEndReason"              -> None,
        "participant1EndDate"                -> None,
        "otherParticipantInstanceIdentifier" -> participiant0Cid,
        "otherParticipantUpdateTimestamp"    -> participiant0Ts
      )

      testStubDataFromAPI(list.head, expectedOutputMap)

      expectedOutputMap = Map(
        "participant"                        -> "Recipient",
        "creationTimestamp"                  -> "20150531235901",
        "participant1StartDate"              -> "20011230",
        "relationshipEndReason"              -> "DEATH",
        "participant1EndDate"                -> "20101230",
        "otherParticipantInstanceIdentifier" -> participiant1Cid,
        "otherParticipantUpdateTimestamp"    -> participiant1Ts
      )

      testStubDataFromAPI(list(1), expectedOutputMap)

      val userRecord                       = relationshipRecordStatusWrapper.relationship_record.userRecord
      val expectedOutput: Map[String, Any] = Map("cid" -> testCid, "timestamp" -> testTs)

      testUserRecord(userRecord, expectedOutput)
    }

    "check for no relationship" in {
      val request = FakeRequest()

      val testData = TestData.Lists.noRelations
      val testNino = Nino(testData.user.nino)
      val testCid  = testData.user.cid.cid
      val testTs   = testData.user.timestamp

      val userRecord1 = UserRecord(testCid, testTs)

      when(mockMarriageAllowanceService.listRelationship(any())(any(), any()))
        .thenReturn(Future.successful(RelationshipRecordWrapper(Seq(), Some(userRecord1))))

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json                            = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[TestRelationshipRecordStatusWrapper]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
      val list = relationshipRecordStatusWrapper.relationship_record.relationshipRecordList

      list.size shouldBe 0

      val userRecord                       = relationshipRecordStatusWrapper.relationship_record.userRecord
      val expectedOutput: Map[String, Any] = Map("cid" -> testCid, "timestamp" -> testTs)

      testUserRecord(userRecord, expectedOutput)
    }

    "check for historic relationship" in {
      val request = FakeRequest()

      val testData = TestData.Lists.oneHistoric
      val testNino = Nino(testData.user.nino)
      val testCid  = testData.user.cid.cid
      val testTs   = testData.user.timestamp

      val participiant0            = testData.counterparties(0)
      val participiant0Cid: String = participiant0.partner.cid.cid.toString
      val participiant0Ts: String  = participiant0.partner.timestamp
      val userRecord1              = UserRecord(testCid, testTs)
      val relRecordP0              = RelationshipRecord(
        "Recipient",
        "20150531235901",
        "20011230",
        Some(RelationshipEndReason.Cancelled),
        Some("20101230"),
        participiant0Cid,
        participiant0Ts
      )

      when(mockMarriageAllowanceService.listRelationship(any())(any(), any()))
        .thenReturn(Future.successful(RelationshipRecordWrapper(Seq(relRecordP0), Some(userRecord1))))

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json                            = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[TestRelationshipRecordStatusWrapper]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
      val list = relationshipRecordStatusWrapper.relationship_record.relationshipRecordList

      list.size shouldBe 1
      val expectedOutputMap = Map(
        "participant"                        -> "Recipient",
        "creationTimestamp"                  -> "20150531235901",
        "participant1StartDate"              -> "20011230",
        "relationshipEndReason"              -> "CANCELLED",
        "participant1EndDate"                -> "20101230",
        "otherParticipantInstanceIdentifier" -> participiant0Cid,
        "otherParticipantUpdateTimestamp"    -> participiant0Ts
      )

      testStubDataFromAPI(list.head, expectedOutputMap)
      list.head.relationshipEndReason.get shouldNot be(null)

      val userRecord                       = relationshipRecordStatusWrapper.relationship_record.userRecord
      val expectedOutput: Map[String, Any] = Map("cid" -> testCid, "timestamp" -> testTs)

      testUserRecord(userRecord, expectedOutput)
    }

    "check for active relationship" in {
      val request = FakeRequest()

      val testData = TestData.Lists.oneActive
      val testNino = Nino(testData.user.nino)
      val testCid  = testData.user.cid.cid
      val testTs   = testData.user.timestamp

      val participiant0            = testData.counterparties(0)
      val participiant0Cid: String = participiant0.partner.cid.cid.toString
      val participiant0Ts          = participiant0.partner.timestamp
      val userRecord1              = UserRecord(testCid, testTs)
      val relRecordP0              =
        RelationshipRecord("Recipient", "20150531235901", "20011230", None, None, participiant0Cid, participiant0Ts)

      when(mockMarriageAllowanceService.listRelationship(any())(any(), any()))
        .thenReturn(Future.successful(RelationshipRecordWrapper(Seq(relRecordP0), Some(userRecord1))))

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe OK

      val json                            = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[TestRelationshipRecordStatusWrapper]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
      val list = relationshipRecordStatusWrapper.relationship_record.relationshipRecordList

      list.size shouldBe 1
      val expectedOutputMap = Map(
        "participant"                        -> "Recipient",
        "creationTimestamp"                  -> "20150531235901",
        "participant1StartDate"              -> "20011230",
        "relationshipEndReason"              -> None,
        "participant1EndDate"                -> None,
        "otherParticipantInstanceIdentifier" -> participiant0Cid,
        "otherParticipantUpdateTimestamp"    -> participiant0Ts
      )

      testStubDataFromAPI(list.head, expectedOutputMap)
      list.head.relationshipEndReason should be(None)

      val userRecord                       = relationshipRecordStatusWrapper.relationship_record.userRecord
      val expectedOutput: Map[String, Any] = Map("cid" -> testCid, "timestamp" -> testTs)

      testUserRecord(userRecord, expectedOutput)

    }

    "return TRANSFEROR-NOT-FOUND when transferor is deceased" in {
      val request = FakeRequest()

      val testData = TestData.Lists.deceased
      val testNino = Nino(testData.user.nino)

      when(mockMarriageAllowanceService.listRelationship(any())(any(), any()))
        .thenReturn(Future.failed(TransferorDeceasedError("Person you're looking for is deceased.")))

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe NOT_FOUND

      val json = Json.parse(contentAsString(result))
      (json \ "status" \ "status_code").get.toString shouldBe "\"TAMC:ERROR:TRANSFEROR-NOT-FOUND\""
    }

    "return TRANSFEROR-NOT-FOUND when transferor not found" in {
      val request = FakeRequest()

      val testData = TestData.Lists.tansfrorNotFound
      val testNino = Nino(testData.user.nino)

      when(mockMarriageAllowanceService.listRelationship(any())(any(), any()))
        .thenReturn(Future.failed(FindRecipientError(1, 1)))

      val result = controller.listRelationship(testNino)(request)
      status(result) shouldBe NOT_FOUND

      val json = Json.parse(contentAsString(result))
      (json \ "status" \ "status_code").get.toString shouldBe "\"TAMC:ERROR:TRANSFEROR-NOT-FOUND\""
    }

  }

  private def testStubDataFromAPI(result: TestRelationshipRecord, expectedOutputMap: Map[String, Any]) = {
    result.participant                           shouldBe expectedOutputMap("participant")
    result.creationTimestamp                     shouldBe expectedOutputMap("creationTimestamp")
    result.participant1StartDate                 shouldBe expectedOutputMap("participant1StartDate")
    result.relationshipEndReason.getOrElse(None) shouldBe expectedOutputMap("relationshipEndReason")
    result.participant1EndDate.getOrElse(None)   shouldBe expectedOutputMap("participant1EndDate")
    result.otherParticipantInstanceIdentifier    shouldBe expectedOutputMap("otherParticipantInstanceIdentifier")
    result.otherParticipantUpdateTimestamp       shouldBe expectedOutputMap("otherParticipantUpdateTimestamp")
  }

  private def testUserRecord(result: UserRecord, expectedOutputMap: Map[String, Any]) = {
    result.cid       shouldBe expectedOutputMap("cid")
    result.timestamp shouldBe expectedOutputMap("timestamp")
  }

  "Calling update relationship for logged in person" should {

    "check if update (cancel) relationship for transferor then response is successfull" in {

      val testInput      = TestData.Updates.cancel
      val transferorNino = Nino(testInput.transferor.nino)
      val recipientCid   = testInput.recipient.cid.cid
      val transferorTs   = testInput.transferor.timestamp
      val recipientTs    = testInput.recipient.timestamp

      val testData                  =
        s"""{"request":{"participant1":{"instanceIdentifier":"$recipientCid","updateTimestamp":"$recipientTs"},"participant2":{"updateTimestamp":"$transferorTs"},"relationship":{"creationTimestamp":"20150531235901","relationshipEndReason":"Cancelled by Transferor","actualEndDate":"20101230"}},"notification":{"full_name":"UNKNOWN","email":"example@example.com","role":"Transferor", "welsh":false, "isRetrospective":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))

      when(mockMarriageAllowanceService.updateRelationship(any())(any(), any())).thenReturn(Future.successful(()))

      val result = controller.updateRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json                            = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[UpdateRelationshipResponse]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
    }

    "check if update (reject) relationship for recipient then response is successfull" in {

      val testInput      = TestData.Updates.reject
      val transferorNino = Nino(testInput.transferor.nino)
      val recipientCid   = testInput.recipient.cid.cid
      val transferorTs   = testInput.transferor.timestamp
      val recipientTs    = testInput.recipient.timestamp

      val testData                  =
        s"""{"request":{"participant1":{"instanceIdentifier":"$recipientCid","updateTimestamp":"$recipientTs"},"participant2":{"updateTimestamp":"$transferorTs"},"relationship":{"creationTimestamp":"20150531235901","relationshipEndReason":"Rejected by Recipient","actualEndDate":"20101230"}},"notification":{"full_name":"UNKNOWN","email":"example@example.com","role":"Recipient", "welsh":false, "isRetrospective":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))

      when(mockMarriageAllowanceService.updateRelationship(any())(any(), any())).thenReturn(Future.successful(()))

      val result = controller.updateRelationship(transferorNino)(request)

      status(result) shouldBe OK

      val json                            = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[UpdateRelationshipResponse]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
    }

    "check if update (divorce) relationship for transferor then response is successfull" in {

      val testInput      = TestData.Updates.divorceTr
      val transferorNino = Nino(testInput.transferor.nino)
      val recipientCid   = testInput.recipient.cid.cid
      val transferorTs   = testInput.transferor.timestamp.toString
      val recipientTs    = testInput.recipient.timestamp.toString

      val testData                  =
        s"""{"request":{"participant1":{"instanceIdentifier":"$recipientCid","updateTimestamp":"$recipientTs"},"participant2":{"updateTimestamp":"$transferorTs"},"relationship":{"creationTimestamp":"20150531235901","relationshipEndReason":"Divorce/Separation","actualEndDate":"20101230"}},"notification":{"full_name":"UNKNOWN","email":"example@example.com","role":"Transferor", "welsh":false, "isRetrospective":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))

      when(mockMarriageAllowanceService.updateRelationship(any())(any(), any())).thenReturn(Future.successful(()))

      val result = controller.updateRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json                            = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[UpdateRelationshipResponse]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
    }

    "check if update (divorce) relationship for recipient then response is successfull" in {

      val testInput      = TestData.Updates.divorceRec
      val transferorNino = Nino(testInput.transferor.nino)
      val recipientCid   = testInput.recipient.cid.cid
      val transferorTs   = testInput.transferor.timestamp
      val recipientTs    = testInput.recipient.timestamp

      val testData                  =
        s"""{"request":{"participant1":{"instanceIdentifier":"$recipientCid","updateTimestamp":"$recipientTs"},"participant2":{"updateTimestamp":"$transferorTs"},"relationship":{"creationTimestamp":"20150531235901","relationshipEndReason":"Divorce/Separation","actualEndDate":"20101230"}},"notification":{"full_name":"UNKNOWN","email":"example@example.com","role":"Recipient", "welsh":false, "isRetrospective":false}}"""
      val request: Request[JsValue] = FakeRequest().withBody(Json.parse(testData))

      when(mockMarriageAllowanceService.updateRelationship(any())(any(), any())).thenReturn(Future.successful(()))

      val result = controller.updateRelationship(transferorNino)(request)
      status(result) shouldBe OK

      val json                            = Json.parse(contentAsString(result))
      val relationshipRecordStatusWrapper = json.as[UpdateRelationshipResponse]
      relationshipRecordStatusWrapper.status.status_code shouldBe "OK"
    }
  }
}
