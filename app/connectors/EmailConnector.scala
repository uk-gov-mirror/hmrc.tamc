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
import models.SendEmailRequest
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpReads, HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class EmailConnector @Inject()(http: HttpClientV2, appConfig: ApplicationConfig) {

  def sendEmail(sendEmailRequest: SendEmailRequest)
               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[UpstreamErrorResponse, Unit]] =
    http
      .post(url"${appConfig.EMAIL_URL}/hmrc/email")
      .withBody(Json.toJson(sendEmailRequest))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(_) => Right(())
        case Left(error) => Left(error)
      }
      .recover {
        case error: HttpException => Left(UpstreamErrorResponse(error.message, BAD_GATEWAY))
      }

  implicit val reads: HttpReads[Either[UpstreamErrorResponse, HttpResponse]] =
    readEitherOf[HttpResponse]
}
