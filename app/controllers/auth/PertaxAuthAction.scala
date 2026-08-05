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

package controllers.auth

import com.google.inject.Inject
import connectors.PertaxConnector
import play.api.Logging
import play.api.http.Status.UNAUTHORIZED
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthAction @Inject() (
  pertaxConnector: PertaxConnector,
  cc: ControllerComponents
) extends ActionFilter[Request]
    with ActionBuilder[Request, AnyContent]
    with Results
    with I18nSupport
    with Logging {

  override def messagesApi: MessagesApi = cc.messagesApi

  override def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequest(request)

    pertaxConnector.authorise.fold(
      {
        case UpstreamErrorResponse(_, status, _, _) if status == UNAUTHORIZED =>
          Some(Unauthorized(""))
        case UpstreamErrorResponse(_, status, _, _) if status >= 499          =>
          Some(BadGateway("Dependant services failing"))
        case _                                                                =>
          Some(InternalServerError("Unexpected response from pertax"))
      },
      {
        case PertaxAuthResponse("ACCESS_GRANTED", _) =>
          None
        case PertaxAuthResponse(code, message)       =>
          Some(Unauthorized(s"Unauthorized - error code: $code message $message"))
      }
    )
  }

  override protected implicit val executionContext: ExecutionContext =
    cc.executionContext

  override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser
}
