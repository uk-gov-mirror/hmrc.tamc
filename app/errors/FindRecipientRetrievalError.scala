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

package errors

sealed trait DataRetrievalError

sealed trait StatusError extends DataRetrievalError
case object BadRequestError extends StatusError
case object ServerError extends StatusError
case object ServiceUnavailableError extends StatusError
case object ResponseValidationError extends StatusError
case object TooManyRequestsError extends StatusError
case object TimeOutError extends StatusError
case object BadGatewayError extends StatusError
case object UnhandledStatusError extends StatusError

case class FindRecipientCodedErrorResponse(returnCode: Int, reasonCode: Int, message: String)
    extends DataRetrievalError {
  def errorMessage: String =
    s"A FindRecipient error has occurred: returnCode:$returnCode, reasonCode:$reasonCode, message=$message"
}
