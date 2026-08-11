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

case class FindTransferorError(returnCode: Int, reasonCode: Int) extends ServiceError(returnCode, reasonCode)
case class FindRecipientError(returnCode: Int, reasonCode: Int) extends ServiceError(returnCode, reasonCode)
case class CheckRelationshipError(returnCode: Int, reasonCode: Int) extends ServiceError(returnCode, reasonCode)
case class CreateRelationshipError(returnCode: Int, reasonCode: Int) extends ServiceError(returnCode, reasonCode)

sealed abstract class ServiceError(returnCode: Int, reasonCode: Int)
    extends RuntimeException(s"(return_code:$returnCode,reason_code:$reasonCode)")
case class TransferorDeceasedError(message: String) extends RuntimeException(s"error message :$message")
case class RecipientDeceasedError(message: String) extends RuntimeException(s"error message :$message")
case class UpdateRelationshipError(message: String) extends RuntimeException(s"error message :$message")
case class MultiYearCreateRelationshipError(message: String) extends RuntimeException(s"error message :$message")

case class RelationshipMightBeCreatedError(message: String) extends RuntimeException(s"error message :$message")

object ErrorResponseStatus {

  val CITIZEN_NOT_FOUND          = "TAMC:ERROR:CITIZEN-NOT-FOUND"
  val BAD_REQUEST                = "TAMC:ERROR:BAD-REQUEST"
  val SERVER_ERROR               = "ERROR:500"
  val SERVICE_UNAVILABLE         = "ERROR:503"
  val CANNOT_UPDATE_RELATIONSHIP = "TAMC:ERROR:CANNOT-UPDATE-RELATIONSHIP"
  val TRANSFEROR_NOT_FOUND       = "TAMC:ERROR:TRANSFEROR-NOT-FOUND"
  val RELATION_MIGHT_BE_CREATED  = "TAMC:ERROR:RELATION-MIGHT-BE-CREATED"
  val RECIPIENT_DECEASED         = "TAMC:ERROR:RECIPIENT-DECEASED"
  val OTHER_ERROR                = "TAMC:ERROR:OTHER-ERROR"
  val TRANSFERER_DECEASED        = "TAMC:ERROR:TRANSFERER-DECEASED"
  val RECIPIENT_NOT_FOUND        = "TAMC:ERROR:RECIPIENT-NOT-FOUND"
}
