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

import models.RelationshipEndReason.RelationshipEndReasonHodsReads
import play.api.libs.json.*

case class RelationshipRecordWrapper(
  relationshipRecordList: Seq[RelationshipRecord],
  userRecord: Option[UserRecord] = None
)

case class RelationshipRecord(
  participant: String,
  creationTimestamp: String,
  participant1StartDate: String,
  relationshipEndReason: Option[RelationshipEndReason] = None,
  participant1EndDate: Option[String] = None,
  otherParticipantInstanceIdentifier: String,
  otherParticipantUpdateTimestamp: String
) {

  def this(
    participant: Int,
    creationTimestamp: String,
    participant1StartDate: String,
    relationshipEndReason: Option[RelationshipEndReason],
    participant1EndDate: Option[String],
    otherParticipantInstanceIdentifier: String,
    otherParticipantUpdateTimestamp: String
  ) =
    this(
      if (participant == 1) "Recipient"
      else "Transferor",
      creationTimestamp,
      participant1StartDate,
      relationshipEndReason,
      participant1EndDate,
      otherParticipantInstanceIdentifier,
      otherParticipantUpdateTimestamp
    )
}

object RelationshipRecord {

  implicit object RelationshipRecordFormat extends Format[RelationshipRecord] {
    def reads(json: JsValue): JsResult[RelationshipRecord] = JsSuccess(
      new RelationshipRecord(
        (json \ "participant").as[Int],
        (json \ "creationTimestamp").as[String],
        (json \ "participant1StartDate").as[String],
        (json \ "relationshipEndReason").asOpt[RelationshipEndReason](RelationshipEndReasonHodsReads),
        (json \ "participant1EndDate").asOpt[String],
        (json \ "otherParticipantInstanceIdentifier").as[String],
        (json \ "otherParticipantUpdateTimestamp").as[String]
      )
    )

    def writes(relationshipRecord: RelationshipRecord): JsValue = Json.obj(
      "participant"                        -> relationshipRecord.participant,
      "creationTimestamp"                  -> relationshipRecord.creationTimestamp,
      "participant1StartDate"              -> relationshipRecord.participant1StartDate,
      "relationshipEndReason"              -> Json.toJson(relationshipRecord.relationshipEndReason),
      "participant1EndDate"                -> relationshipRecord.participant1EndDate,
      "otherParticipantInstanceIdentifier" -> relationshipRecord.otherParticipantInstanceIdentifier,
      "otherParticipantUpdateTimestamp"    -> relationshipRecord.otherParticipantUpdateTimestamp
    )
  }
}

object RelationshipRecordWrapper {

  implicit object RelationshipRecordListFormat extends Format[RelationshipRecordWrapper] {
    def reads(json: JsValue): JsResult[RelationshipRecordWrapper]             = JsSuccess(
      RelationshipRecordWrapper((json \ "relationships").as[Seq[RelationshipRecord]])
    )
    def writes(relatisoshipRecordWrapper: RelationshipRecordWrapper): JsValue = Json.obj(
      "relationships" -> relatisoshipRecordWrapper.relationshipRecordList,
      "userRecord"    -> relatisoshipRecordWrapper.userRecord
    )
  }
}
