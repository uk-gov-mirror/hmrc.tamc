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

package services

import Fixtures.MultiYearCreateRelationshipRequestHolderFixture
import com.codahale.metrics.Timer
import config.ApplicationConfig
import connectors.{EmailConnector, MarriageAllowanceDESConnector}
import errors.TooManyRequestsError
import metrics.TamcMetrics
import models._
import models.emailAddress.EmailAddress
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Injecting
import test_utils.UnitSpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http._
import uk.gov.hmrc.time.TaxYear

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionException, Future}

class MarriageAllowanceServiceSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting {

  val year: Int                                  = TaxYear.current.startYear
  val generatedNino: Nino                        = new Generator().nextNino
  val cID                                        = 123456789
  val findRecipientRequest: FindRecipientRequest = FindRecipientRequest(
    name = "testForename1",
    lastName = "testLastName",
    gender = Gender("M"),
    nino = generatedNino,
    dateOfMarriage = Some(LocalDate.of(year, 12, 12))
  )
  val userRecord: UserRecord                     = UserRecord(cid = cID, timestamp = "20200116155359011123")

  val mockMarriageAllowanceDESConnector: MarriageAllowanceDESConnector = mock[MarriageAllowanceDESConnector]
  val mockTamcMetrics: TamcMetrics                                     = mock[TamcMetrics]
  val mockEmailConnector: EmailConnector                               = mock[EmailConnector]
  val mockAppConfig: ApplicationConfig                                 = mock[ApplicationConfig]

  val findCitizenJson: JsValue = Json.parse(s"""

  {"Jtpr1311PerDetailsFindcallResponse": {"Jtpr1311PerDetailsFindExport": {
    "@exitStateType": "3",
    "@exitStateMsg": "",
    "@command": "",
    "@exitState": "0",
    "OutItpr1Person":    {
    "InstanceIdentifier": 100013333,
    "UpdateTimestamp": "2015",
    "Nino": "${generatedNino.nino}",
    "NinoStatus": null,
    "TemporaryReference": "00B00004",
    "Surname": "testSurname",
    "FirstForename": "testForeName",
    "SecondForename": "testSecondForename",
    "Initials": "AF",
    "Title": "MR",
    "Honours": "BSC",
    "Sex": "M",
    "DateOfBirth": 19400610,
    "DeceasedSignal": "N",
    "OrgUnitInstance": 0
  },
    "OutGroupOfIrInterestArea": {"row":    [
  {"OutRgItpr1IrInterestArea":       {
    "Code": 2,
    "Description": "PAY AS YOU EARN",
    "Reference": null,
    "Reason": "FOR TEST PAYE",
    "StartDatetime": 2.0000403E19
  }}]},
    "OutGroupOfCommunications": {"row":    [
  {"OutRgItpr1Communication":       {
    "Code": 7,
    "Description": "DAYTIME TELEPHONE",
    "ContactDetails": "00000 290000"
  }}
    ]},
    "OutGroupOfOccupancy": {"row":    [
  {"OutRgItpr1Occupancy":       {
    "StatusCode": null,
    "StatusDescription": null,
    "TypeOfOccupancyCode": 0,
    "TypeOfOccupancyDescription": null,
    "AdditionalDeliveryInformation": null,
    "RlsSignal": null,
    "TypeOfAddressCode": null,
    "TypeOfAddressDescription": null,
    "AddressLine1": null,
    "AddressLine2": null,
    "AddressLine3": null,
    "AddressLine4": null,
    "AddressLine5": null,
    "Postcode": null,
    "HouseIdentification": null,
    "HomeCountry": null,
    "ForeignCountry": null,
    "NoLongerUsedSignal": null
  }}
    ]},
    "OutItpr1Capacitor": {"InstanceIdentifier": 0},
    "OutWCbdParameters":    {
    "SeverityCode": "I",
    "DataStoreStatus": 1,
    "OriginServid": 1011,
    "ContextString": null,
    "ReturnCode": 1,
    "ReasonCode": 1,
    "Checksum": null
  }
  }}}""")

  val listRelationshipdJson: JsValue = Json.parse("""{
    "relationships": [
    {
      "participant": 1,
      "creationTimestamp": "20150531235901",
      "actualStartDate": "20011230",
      "relationshipEndReason": "Death",
      "participant1StartDate": "20011230",
      "participant2StartDate": "20011230",
      "actualEndDate": "20101230",
      "participant1EndDate": "20101230",
      "participant2EndDate": "20101230",
      "otherParticipantInstanceIdentifier": "123456789012345",
      "otherParticipantUpdateTimestamp": "20150531235901",
      "participant2UKResident": true
    },
    {
      "participant": 1,
      "creationTimestamp": "20150531235901",
      "actualStartDate": "20011230",
      "relationshipEndReason": "Death",
      "participant1StartDate": "20011230",
      "participant2StartDate": "20011230",
      "actualEndDate": "20101230",
      "participant1EndDate": "20101230",
      "participant2EndDate": "20101230",
      "otherParticipantInstanceIdentifier": "123456789012345",
      "otherParticipantUpdateTimestamp": "20150531235901",
      "participant2UKResident": true
    }
    ]
  }""")

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MarriageAllowanceDESConnector].toInstance(mockMarriageAllowanceDESConnector),
      bind[TamcMetrics].toInstance(mockTamcMetrics),
      bind[EmailConnector].toInstance(mockEmailConnector),
      bind[ApplicationConfig].toInstance(mockAppConfig)
    )
    .build()

  def setAppConFigValuesInMock(): Unit = {
    when(mockAppConfig.ROLE_TRANSFEROR).thenReturn("Transferor")
    when(mockAppConfig.ROLE_RECIPIENT).thenReturn("Recipient")

    when(mockAppConfig.REASON_CANCEL).thenReturn("Cancelled by Transferor")
    when(mockAppConfig.REASON_REJECT).thenReturn("Rejected by Recipient")
    when(mockAppConfig.REASON_DIVORCE).thenReturn("Divorce/Separation")

    when(mockAppConfig.START_DATE).thenReturn("6 April ")
    when(mockAppConfig.END_DATE).thenReturn("5 April ")

    when(mockAppConfig.START_DATE_CY).thenReturn("6 Ebrill")
    when(mockAppConfig.END_DATE_CY).thenReturn("5 Ebrill")
  }

  def service: MarriageAllowanceService = inject[MarriageAllowanceService]

  "getRecipientRelationship" should {
    "return a UserRecord, list of TaxYearModel tuple given a valid FindRecipientRequest " in {
      val expectedResponse = (userRecord, List())
      when(mockMarriageAllowanceDESConnector.findRecipient(meq(findRecipientRequest))(any(), any()))
        .thenReturn(Future.successful(Right(userRecord)))

      when(mockMarriageAllowanceDESConnector.findCitizen(meq(generatedNino))(any(), any()))
        .thenReturn(Future.successful(Right(findCitizenJson)))

      when(mockMarriageAllowanceDESConnector.listRelationship(any(), any())(any(), any()))
        .thenReturn(
          Future.successful(Right(listRelationshipdJson))
        )

      when(mockAppConfig.START_TAX_YEAR).thenReturn(2015)
      when(mockAppConfig.MA_SUPPORTED_YEARS_COUNT).thenReturn(5)

      val mockTimerContext = mock[Timer.Context]
      when(mockTamcMetrics.startTimer(any())).thenReturn(mockTimerContext)
      when(mockTimerContext.stop()).thenReturn(123456789L)

      val result = await(service.getRecipientRelationship(generatedNino, findRecipientRequest))

      result shouldBe Right(expectedResponse)
    }

    "return a DataRetrievalError error type based on error returned" in {
      when(mockMarriageAllowanceDESConnector.findRecipient(meq(findRecipientRequest))(any(), any()))
        .thenReturn(Future.successful(Left(TooManyRequestsError)))

      val result = await(service.getRecipientRelationship(generatedNino, findRecipientRequest))

      result shouldBe Left(TooManyRequestsError)
    }
  }

  "when createMultiYearRelationship" should {
    "return unit" when {
      "createRelationshipRequest tax year is none" in {
        when(mockEmailConnector.sendEmail(any())(any(), any())).thenReturn(
          Future.successful(Right(()))
        )

        val multiYearCreateRelationshipRequest =
          MultiYearCreateRelationshipRequestHolderFixture.multiYearCreateRelationshipRequestNoTaxYearHolder
        val response                           = service.createMultiYearRelationship(multiYearCreateRelationshipRequest, "GDS")(
          new HeaderCarrier(),
          implicitly
        )

        await(response) shouldBe a[Unit]
      }
    }

    "when request is sent with deceased recipient in MarriageAllowanceService" should {
      "return a BadRequestException" in {
        when(mockMarriageAllowanceDESConnector.sendMultiYearCreateRelationshipRequest(any(), any())(any(), any()))
          .thenReturn(Future.failed(new BadRequestException("{\"reason\": \"Participant is deceased\"}")))

        val multiYearCreateRelationshipRequest =
          MultiYearCreateRelationshipRequestHolderFixture.multiYearCreateRelationshipRequestHolder
        val response                           = service.createMultiYearRelationship(multiYearCreateRelationshipRequest, "GDS")(
          new HeaderCarrier(),
          implicitly
        )

        intercept[BadRequestException] {
          await(response)
        }
      }
    }

    "when request is sent for current year" should {
      "return unit" in {
        val mockTimerContext = mock[Timer.Context]
        when(mockTamcMetrics.startTimer(any())).thenReturn(mockTimerContext)
        when(mockTimerContext.stop()).thenReturn(123456789L)

        val jsVal: JsValue = Json.parse(""" {
            |"status":"Processing OK",
            |"CID1Timestamp": "123456789",
            |"CID2Timestamp": "123456789" }
          """.stripMargin)

        val resultVal: Future[Either[UpstreamErrorResponse, JsValue]] = Future.successful(Right(jsVal))

        when(mockMarriageAllowanceDESConnector.sendMultiYearCreateRelationshipRequest(any(), any())(any(), any()))
          .thenReturn(resultVal)

        when(mockEmailConnector.sendEmail(any())(any(), any())).thenReturn(Future.successful(Right(())))

        val multiYearCreateRelationshipRequest =
          MultiYearCreateRelationshipRequestHolderFixture.multiYearCreateRelationshipCurrentYearHolder
        val response                           = service.createMultiYearRelationship(multiYearCreateRelationshipRequest, "GDS")(
          new HeaderCarrier(),
          implicitly
        )

        await(response) shouldBe a[Unit]
      }
    }

  }

  "when updateRelationship" should {
    List(
      ("Rejected by Recipient", "Recipient", true, false, true),
      ("Rejected by Recipient", "Recipient", false, false, true),
      ("Rejected by Recipient", "Recipient", false, true, true),
      ("Divorce/Separation", "Transferor", false, false, true),
      ("Divorce/Separation", "Transferor", false, false, false),
      ("Divorce/Separation", "Recipient", false, false, true),
      ("Divorce/Separation", "Recipient", false, false, false),
      ("Cancelled by Transferor", "Transferor", false, false, true)
    ).foreach { case (reason, role, welsh, retrospective, current) =>
      s"updating for $reason $role ${if (welsh) "Welsh" else "English"} ${if (retrospective) "retrospectively" else ""} " +
        s"${if (current) "Current" else "Previous"} tax year" in {
          setAppConFigValuesInMock()
          val mockTimerContext = mock[Timer.Context]
          when(mockTamcMetrics.startTimer(any())).thenReturn(mockTimerContext)
          when(mockTimerContext.stop()).thenReturn(123456789L)

          val sdf     = new SimpleDateFormat("yyyyMMdd")
          val theDate = Calendar.getInstance()
          if (!current) theDate.add(Calendar.YEAR, -1)
          val endDate = sdf.format(theDate.getTime)

          val desUpdateRelationshipRequest: DesUpdateRelationshipRequest = DesUpdateRelationshipRequest(
            new DesRecipientInformation("participant1", theDate.toString),
            new DesTransferorInformation("participant2"),
            new DesRelationshipInformation(theDate.toString, reason, endDate)
          )

          val notification: UpdateRelationshipNotificationRequest = new UpdateRelationshipNotificationRequest(
            "Fred Bloggs",
            new EmailAddress("fred@bloggs.com"),
            role,
            welsh,
            retrospective
          )

          val updateRelationshipRequestHolder: UpdateRelationshipRequestHolder =
            new UpdateRelationshipRequestHolder(desUpdateRelationshipRequest, notification)

          when(mockMarriageAllowanceDESConnector.updateAllowanceRelationship(any())(any(), any()))
            .thenReturn(Future.successful(Right(())))

          when(mockEmailConnector.sendEmail(any())(any(), any())).thenReturn(Future.successful(Right(())))

          val response = service.updateRelationship(updateRelationshipRequestHolder)(new HeaderCarrier(), implicitly)
          await(response) shouldBe a[Unit]
        }
    }
  }

  "when updateRelationship with invalid role" should {
    "return a NotImplementedError" in {
      setAppConFigValuesInMock()
      val mockTimerContext = mock[Timer.Context]
      when(mockTamcMetrics.startTimer(any())).thenReturn(mockTimerContext)
      when(mockTimerContext.stop()).thenReturn(123456789L)

      val sdf     = new SimpleDateFormat("yyyyMMdd")
      val theDate = Calendar.getInstance()
      val endDate = sdf.format(theDate.getTime)

      val desUpdateRelationshipRequest: DesUpdateRelationshipRequest = DesUpdateRelationshipRequest(
        new DesRecipientInformation("participant1", theDate.toString),
        new DesTransferorInformation("participant2"),
        new DesRelationshipInformation(theDate.toString, "Divorce/Separation", endDate)
      )

      val notification: UpdateRelationshipNotificationRequest = new UpdateRelationshipNotificationRequest(
        "Fred Bloggs",
        new EmailAddress("fred@bloggs.com"),
        "INVALID",
        false,
        false
      )

      val updateRelationshipRequestHolder: UpdateRelationshipRequestHolder =
        new UpdateRelationshipRequestHolder(desUpdateRelationshipRequest, notification)

      when(mockMarriageAllowanceDESConnector.updateAllowanceRelationship(any())(any(), any()))
        .thenReturn(Future.successful(Right(())))

      when(mockEmailConnector.sendEmail(any())(any(), any())).thenReturn(Future.successful(Right(())))

      val response = service.updateRelationship(updateRelationshipRequestHolder)(new HeaderCarrier(), implicitly)

      val exceptionThrown: ExecutionException = intercept[ExecutionException] {
        await(response)
      }
      exceptionThrown.getCause.getMessage == "reason and role not handled: Divorce/Separation, INVALID"
    }
  }

  "when getting relationship list" should {
    "return RelationshipRecordWrapper" when {
      "for generated NINO in " in {
        val mockTimerContext = mock[Timer.Context]
        when(mockTamcMetrics.startTimer(any())).thenReturn(mockTimerContext)
        when(mockTimerContext.stop()).thenReturn(123456789L)

        when(mockMarriageAllowanceDESConnector.findCitizen(meq(generatedNino))(any(), any()))
          .thenReturn(Future.successful(Right(findCitizenJson)))

        when(mockMarriageAllowanceDESConnector.listRelationship(any(), any())(any(), any()))
          .thenReturn(
            Future.successful(Right(listRelationshipdJson))
          )

        val response = service.listRelationship(generatedNino)
        await(response) shouldBe a[models.RelationshipRecordWrapper]
      }
    }
  }

}
