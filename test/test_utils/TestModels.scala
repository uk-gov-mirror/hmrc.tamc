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

package test_utils

import models.{ResponseStatus, UserRecord}
import play.api.libs.json.{Format, JsSuccess, JsValue, Json, OFormat}

case class TestRelationshipRecordWrapper(relationshipRecordList: Seq[TestRelationshipRecord], userRecord: UserRecord)
case class TestRelationshipRecord(
  participant: String,
  creationTimestamp: String,
  participant1StartDate: String,
  relationshipEndReason: Option[String] = None,
  participant1EndDate: Option[String] = None,
  otherParticipantInstanceIdentifier: String,
  otherParticipantUpdateTimestamp: String
)
case class TestRelationshipRecordStatusWrapper(
  relationship_record: TestRelationshipRecordWrapper = null,
  status: ResponseStatus
)

object TestRelationshipRecord {

  implicit object TestRelationshipRecordFormat extends Format[TestRelationshipRecord] {
    def reads(json: JsValue) = JsSuccess(
      new TestRelationshipRecord(
        (json \ "participant").as[String],
        (json \ "creationTimestamp").as[String],
        (json \ "participant1StartDate").as[String],
        (json \ "relationshipEndReason").asOpt[String],
        (json \ "participant1EndDate").asOpt[String],
        (json \ "otherParticipantInstanceIdentifier").as[String],
        (json \ "otherParticipantUpdateTimestamp").as[String]
      )
    )

    def writes(relatisoshipRecord: TestRelationshipRecord) = Json.obj(
      "participant"                        -> relatisoshipRecord.participant,
      "creationTimestamp"                  -> relatisoshipRecord.creationTimestamp,
      "participant1StartDate"              -> relatisoshipRecord.participant1StartDate,
      "relationshipEndReason"              -> relatisoshipRecord.relationshipEndReason,
      "participant1EndDate"                -> relatisoshipRecord.participant1EndDate,
      "otherParticipantInstanceIdentifier" -> relatisoshipRecord.otherParticipantInstanceIdentifier,
      "otherParticipantUpdateTimestamp"    -> relatisoshipRecord.otherParticipantUpdateTimestamp
    )
  }
}

object TestRelationshipRecordWrapper {

  implicit object RelationshipRecordListFormat extends Format[TestRelationshipRecordWrapper] {
    def reads(json: JsValue)                                             = JsSuccess(
      TestRelationshipRecordWrapper(
        (json \ "relationships").as[Seq[TestRelationshipRecord]],
        (json \ "userRecord").as[UserRecord]
      )
    )
    def writes(relatisoshipRecordWrapper: TestRelationshipRecordWrapper) = Json.obj(
      "relationships" -> relatisoshipRecordWrapper.relationshipRecordList,
      "userRecord"    -> relatisoshipRecordWrapper.userRecord
    )
  }
}

object TestRelationshipRecordStatusWrapper {
  implicit val formats: OFormat[TestRelationshipRecordStatusWrapper] = Json.format[TestRelationshipRecordStatusWrapper]
}
