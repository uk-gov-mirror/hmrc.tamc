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

package connectors

import com.google.inject.Inject
import config.ApplicationConfig
import errors.*
import metrics.TamcMetrics
import models.*
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class MarriageAllowanceDESConnector @Inject()(val metrics: TamcMetrics,
                                              http: HttpClientV2,
                                              appConfig: ApplicationConfig)
  extends MarriageAllowanceConnector with Logging {

  override val serviceUrl: String = appConfig.serviceUrl
  override val urlHeaderEnvironment: String = appConfig.urlHeaderEnvironment
  override val urlHeaderAuthorization = s"Bearer ${appConfig.urlHeaderAuthorization}"

  def findCitizen(nino: Nino)(
    implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, JsValue]] = {
    val path = url"$serviceUrl/marriage-allowance/citizen/$nino"
    http
      .get(path)
      .setHeader(explicitHeaders*)
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(response) => Right(response.json)
        case Left(error) => Left(error)
      }
      .recover {
        case error: HttpException => Left(UpstreamErrorResponse(error.message, BAD_GATEWAY))
      }
  }

  def listRelationship(cid: Cid, includeHistoric: Boolean = true)(
    implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, JsValue]] = {
    val path = url"$serviceUrl/marriage-allowance/citizen/$cid/relationships?includeHistoric=$includeHistoric"
    http
      .get(path)
      .setHeader(explicitHeaders*)
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(response) => Right(response.json)
        case Left(error) => Left(error)
      }
      .recover {
        case error: HttpException => Left(UpstreamErrorResponse(error.message, BAD_GATEWAY))
      }
  }

  private def generateResponse( codedErrorResponse: FindRecipientCodedErrorResponse) = {
    logger.warn(codedErrorResponse.errorMessage)
    metrics.incrementSuccessCounter(ApiType.FindRecipient)
    Left(codedErrorResponse)
  }

  private def evaluateCodes(findRecipientResponseDES: FindRecipientResponseDES): Either[DataRetrievalError, UserRecord] = {
    (findRecipientResponseDES.returnCode, findRecipientResponseDES.reasonCode) match {
      case(ProcessingOK, ProcessingOK) =>
        metrics.incrementSuccessCounter(ApiType.FindRecipient)
        Right(UserRecord(findRecipientResponseDES.instanceIdentifier, findRecipientResponseDES.updateTimeStamp))
      case codes @ (ErrorReturnCode, NinoNotFound) =>
        generateResponse(FindRecipientCodedErrorResponse(codes._1, codes._2, "Nino not found and Nino not found in merge trail"))
      case codes @ (ErrorReturnCode, MultipleNinosInMergeTrail) =>
        generateResponse(FindRecipientCodedErrorResponse(codes._1, codes._2, "Nino not found and Nino found in multiple merge trails"))
      case codes @ (ErrorReturnCode, ConfidenceCheck) =>
        generateResponse(FindRecipientCodedErrorResponse(codes._1, codes._2, "Confidence check failed"))
      case codes @ (ErrorReturnCode, NinoRequired) =>
        generateResponse(FindRecipientCodedErrorResponse(codes._1, codes._2, "Nino must be supplied"))
      case codes @ (ErrorReturnCode, OnlyOneNinoOrTempReference) =>
        generateResponse(FindRecipientCodedErrorResponse(codes._1, codes._2, "Only one of Nino or Temporary Reference must be supplied"))
      case codes @ (ErrorReturnCode, SurnameNotSupplied) =>
        generateResponse(FindRecipientCodedErrorResponse(codes._1, codes._2, "Confidence Check Surname not supplied"))
      case(returnCode, reasonCode) =>
        logger.error(s"Unknown response code returned from DES: ReturnCode=$returnCode, ReasonCode=$reasonCode")
        metrics.incrementFailedCounter(ApiType.FindRecipient)
        Left(UnhandledStatusError)
    }
  }

  implicit val httpRead: HttpReads[Either[DataRetrievalError, UserRecord]] =
    (_: String, _: String, response: HttpResponse) =>
      response.status match {
        case OK =>
          response.json.validate[FindRecipientResponseDES].fold(handleValidationError, evaluateCodes)
        case BAD_REQUEST =>
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          Left(BadRequestError)
        case TOO_MANY_REQUESTS =>
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          Left(TooManyRequestsError)
        case INTERNAL_SERVER_ERROR =>
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          logger.error(s" Internal Server Error received from DES: ${response.body}")
          Left(ServerError)
        case SERVICE_UNAVAILABLE =>
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          logger.error("Service Unavailable returned from DES")
          Left(ServiceUnavailableError)
        case 499 | GATEWAY_TIMEOUT =>
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          logger.error("Timeout Error has been received from DES")
          Left(TimeOutError)
        case BAD_GATEWAY =>
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          logger.error("Bad Gateway Error returned from DES")
          Left(BadGatewayError)
        case _ =>
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          logger.error(s"Des has returned Unhandled Status Code: ${response.status}")
          Left(UnhandledStatusError)
      }

  def findRecipient(findRecipientRequest: FindRecipientRequest)
                   (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[DataRetrievalError, UserRecord]] = {

    val nino = ninoWithoutSpaces(findRecipientRequest.nino)

    val path = url"$serviceUrl/marriage-allowance/citizen/$nino/check"
    val findRecipientRequestDes = FindRecipientRequestDes(findRecipientRequest)

    metrics.incrementTotalCounter(ApiType.FindRecipient)
    val timer = metrics.startTimer(ApiType.FindRecipient)

    http
      .post(path)
      .withBody(Json.toJson(findRecipientRequestDes))
      .setHeader(explicitHeaders*)
      .execute[Either[DataRetrievalError, UserRecord]]
      .map { response =>
        timer.stop()
        response
      } recover {
        case _: GatewayTimeoutException =>
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          timer.stop()
          Left(TimeOutError)
        case _: BadGatewayException =>
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          timer.stop()
          Left(BadGatewayError)
        case NonFatal(e) =>
          metrics.incrementFailedCounter(ApiType.FindRecipient)
          timer.stop()
          throw e
      }
  }

  def sendMultiYearCreateRelationshipRequest(
    relType: String, createRelationshipRequest: MultiYearDesCreateRelationshipRequest)(
    implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, JsValue]] = {
    val path = url"$serviceUrl/marriage-allowance/02.00.00/citizen/${createRelationshipRequest.recipientCid}/relationship/$relType"
    http
      .post(path)
      .withBody(Json.toJson(createRelationshipRequest))
      .setHeader(explicitHeaders*)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => Right(response.json)
          case _ => Left(UpstreamErrorResponse(response.body, response.status))
        }

      }
      .recover {
        case error: HttpException => Left(UpstreamErrorResponse(error.message, BAD_GATEWAY))
      }
  }

  def updateAllowanceRelationship(updateRelationshipRequest: DesUpdateRelationshipRequest)(
    implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] = {
    val path = url"$serviceUrl/marriage-allowance/citizen/${updateRelationshipRequest.participant1.instanceIdentifier}/relationship"
    http
      .put(path)
      .withBody(Json.toJson(updateRelationshipRequest))
      .setHeader(explicitHeaders*)
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(_) => Right(())
        case Left(error) => Left(error)
      }
      .recover {
        case error: HttpException => Left(UpstreamErrorResponse(error.message, BAD_GATEWAY))
      }
  }
}
