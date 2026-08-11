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

import errors.{DataRetrievalError, ResponseValidationError}
import metrics.TamcMetrics
import models._
import play.api.Logging
import play.api.libs.json.{JsPath, JsValue, JsonValidationError}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait MarriageAllowanceConnector extends Logging {

  val ProcessingOK               = 1
  val ErrorReturnCode            = -1011
  val NinoNotFound               = 2016
  val MultipleNinosInMergeTrail  = 2017
  val ConfidenceCheck            = 2018
  val NinoRequired               = 2039
  val OnlyOneNinoOrTempReference = 2040
  val SurnameNotSupplied         = 2061

  val serviceUrl: String
  val urlHeaderEnvironment: String
  val urlHeaderAuthorization: String
  val metrics: TamcMetrics
  def url(path: String)                        = s"$serviceUrl$path"
  def ninoWithoutSpaces(nino: Nino): Timestamp = nino.value.replaceAll(" ", "")

  def handleValidationError[A]
    : scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])] => Either[DataRetrievalError, A] =
    err => {

      val extractValidationErrors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])] => String =
        errors =>
          errors
            .map {
              case (path, List(validationError: JsonValidationError, _*)) => s"$path: ${validationError.message}"
              case (path, err)                                            => s"$path: $err"
            }
            .mkString(", ")
            .trim

      logger.error(s"Not able to parse the response received from DES with error ${extractValidationErrors(err)}")
      Left(ResponseValidationError)
    }

  def explicitHeaders(implicit hc: HeaderCarrier): List[(String, String)] =
    List(
      HeaderNames.authorisation -> urlHeaderAuthorization,
      HeaderNames.xRequestId    -> hc.requestId.fold("-")(_.value),
      HeaderNames.xSessionId    -> hc.sessionId.fold("-")(_.value),
      "Environment"             -> urlHeaderEnvironment,
      "CorrelationId"           -> UUID.randomUUID().toString
    )

  def findCitizen(
    nino: Nino
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, JsValue]]

  def listRelationship(cid: Cid, includeHistoric: Boolean = true)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, JsValue]]

  def findRecipient(
    findRecipientRequest: FindRecipientRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[DataRetrievalError, UserRecord]]

  def sendMultiYearCreateRelationshipRequest(
    relType: String,
    createRelationshipRequest: MultiYearDesCreateRelationshipRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, JsValue]]

  def updateAllowanceRelationship(
    updateRelationshipRequest: DesUpdateRelationshipRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]]

}
