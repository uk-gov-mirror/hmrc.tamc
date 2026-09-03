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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

case class FindRecipientResponseDES(
  reasonCode: Int,
  returnCode: Int,
  instanceIdentifier: Cid,
  updateTimeStamp: Timestamp
)

object FindRecipientResponseDES {
  implicit val reads: Reads[FindRecipientResponseDES] = (
    (JsPath \ "Jfwk1012FindCheckPerNoninocallResponse" \ "Jfwk1012FindCheckPerNoninoExport" \ "OutWCbdParameters" \ "ReasonCode")
      .read[Int] and
      (JsPath \ "Jfwk1012FindCheckPerNoninocallResponse" \ "Jfwk1012FindCheckPerNoninoExport" \ "OutWCbdParameters" \ "ReturnCode")
        .read[Int] and
      (JsPath \ "Jfwk1012FindCheckPerNoninocallResponse" \ "Jfwk1012FindCheckPerNoninoExport" \ "OutItpr1Person" \ "InstanceIdentifier")
        .read[Cid] and
      (JsPath \ "Jfwk1012FindCheckPerNoninocallResponse" \ "Jfwk1012FindCheckPerNoninoExport" \ "OutItpr1Person" \ "UpdateTimestamp")
        .read[Timestamp]
  )(FindRecipientResponseDES.apply)
}
