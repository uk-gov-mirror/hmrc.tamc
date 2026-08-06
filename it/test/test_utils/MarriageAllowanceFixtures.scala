/*
 * Copyright 2024 HM Revenue & Customs
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

import models.Cid
import play.api.libs.json.{JsString, Json}

trait MarriageAllowanceFixtures {

  def getRecipientRelationshipResponse(cid: Cid, reasonCode: Int = 1, returnCode: Int = 1) = Json.parse(s"""{
          "Jfwk1012FindCheckPerNoninocallResponse": {
            "Jfwk1012FindCheckPerNoninoExport": {
              "@exitStateType": "0",
              "@exitState": "0",
              "OutItpr1Person": {
                "InstanceIdentifier": $cid,
                "UpdateTimestamp": "20200116155359011123"
              },
              "OutWCbdParameters": {
                "SeverityCode": "W",
                "DataStoreStatus": "S",
                "OriginServid": 9999,
                "ContextString": "ITPR1311_PER_DETAILS_FIND_S",
                "ReturnCode": $returnCode,
                "ReasonCode": $reasonCode,
                "Checksum": "a234jnjbhr9ui83"
              }
            }
          }
      }""")

  val successPertaxAuthResponse = Json.obj(
    "code"    -> JsString("ACCESS_GRANTED"),
    "message" -> JsString("Some message")
  )

  def findCitizenResponse(cid: Cid, deceasedSignal: String = "N", returnCode: Int = 1, reasonCode: Int = 1) =
    Json.parse(s"""{
        "Jtpr1311PerDetailsFindcallResponse":{
            "Jtpr1311PerDetailsFindExport":{
                "OutItpr1Person":{
                    "InstanceIdentifier": $cid,
                    "UpdateTimestamp":"20200116155359011123",
                    "Surname":"Smith",
                    "FirstForename":"Smith",
                    "DeceasedSignal":"$deceasedSignal"
                },
                "OutWCbdParameters":{
                    "ReturnCode":$returnCode,
                    "ReasonCode":$reasonCode
                }
            }
        }
    }""")

  val listRelationshipResponse = Json.parse("""
      |{
      |  "relationships": [
      |  {
      |   "participant": 1,
      |   "creationTimestamp": "20220531235901",
      |   "actualStartDate": "20220406",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20220531235901",
      |   "participant2UKResident": true
      |  },
      |  {
      |   "participant": 1,
      |   "creationTimestamp": "20220531235902",
      |   "actualStartDate": "20210406",
      |   "relationshipEndReason": "Death (either participant)",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "actualEndDate": "20210406",
      |   "participant1EndDate": "20210406",
      |   "participant2EndDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20220531235901",
      |   "participant2UKResident": true
      |  },
      |  {
      |   "participant": 1,
      |   "creationTimestamp": "20210531235903",
      |   "actualStartDate": "20210406",
      |   "relationshipEndReason": "Relationship Type specific - for future use",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "actualEndDate": "20210406",
      |   "participant1EndDate": "20210406",
      |   "participant2EndDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20210531235901",
      |   "participant2UKResident": true
      |  },
      |  {
      |   "participant": 1,
      |   "creationTimestamp": "20210531235904",
      |   "actualStartDate": "20210406",
      |   "relationshipEndReason": "Invalid Participant",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "actualEndDate": "20210406",
      |   "participant1EndDate": "20210406",
      |   "participant2EndDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20210531235901",
      |   "participant2UKResident": true
      |  },
      |  {
      |   "participant": 1,
      |   "creationTimestamp": "20210531235905",
      |   "actualStartDate": "20210406",
      |   "relationshipEndReason": "Retrospective",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "actualEndDate": "20210406",
      |   "participant1EndDate": "20210406",
      |   "participant2EndDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20210531235901",
      |   "participant2UKResident": true
      |  },
      |
      |  {
      |   "participant": 1,
      |   "creationTimestamp": "20210531235906",
      |   "actualStartDate": "20210406",
      |   "relationshipEndReason": "Ended by HMRC",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "actualEndDate": "20210406",
      |   "participant1EndDate": "20210406",
      |   "participant2EndDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20210531235901",
      |   "participant2UKResident": true
      |  },
      |  {
      |   "participant": 2,
      |   "creationTimestamp": "20210531235907",
      |   "actualStartDate": "20210406",
      |   "relationshipEndReason": "Merger",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "actualEndDate": "20210406",
      |   "participant1EndDate": "20210406",
      |   "participant2EndDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20210531235901",
      |   "participant2UKResident": true
      |  },
      |  {
      |   "participant": 1,
      |   "creationTimestamp": "20210531235908",
      |   "actualStartDate": "20210406",
      |   "relationshipEndReason": "Closed by mutual consent of participants",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "actualEndDate": "20210406",
      |   "participant1EndDate": "20210406",
      |   "participant2EndDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20210531235901",
      |   "participant2UKResident": true
      |  },
      |    {
      |   "participant": 1,
      |   "creationTimestamp": "20210531235909",
      |   "actualStartDate": "20210406",
      |   "relationshipEndReason": "Ended by Participant 2",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "actualEndDate": "20210406",
      |   "participant1EndDate": "20210406",
      |   "participant2EndDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20210531235901",
      |   "participant2UKResident": true
      |  },
      |    {
      |   "participant": 1,
      |   "creationTimestamp": "20210531235910",
      |   "actualStartDate": "20210406",
      |   "relationshipEndReason": "Divorce/Separation",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "actualEndDate": "20210406",
      |   "participant1EndDate": "20210406",
      |   "participant2EndDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20210531235901",
      |   "participant2UKResident": true
      |  },
      |  {
      |   "participant": 1,
      |   "creationTimestamp": "20210531235911",
      |   "actualStartDate": "20210406",
      |   "relationshipEndReason": "System Closure",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "actualEndDate": "20210406",
      |   "participant1EndDate": "20210406",
      |   "participant2EndDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20210531235901",
      |   "participant2UKResident": true
      |  },
      |    {
      |   "participant": 1,
      |   "creationTimestamp": "20210531235912",
      |   "actualStartDate": "20210406",
      |   "relationshipEndReason": "Active",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "actualEndDate": "20210406",
      |   "participant1EndDate": "20210406",
      |   "participant2EndDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20210531235901",
      |   "participant2UKResident": true
      |  },
      |    {
      |   "participant": 1,
      |   "creationTimestamp": "20210531235913",
      |   "actualStartDate": "20210406",
      |   "relationshipEndReason": "Ineligible Participant (for MA this is MCA)",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "actualEndDate": "20210406",
      |   "participant1EndDate": "20210406",
      |   "participant2EndDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20210531235901",
      |   "participant2UKResident": true
      |  },
      |    {
      |   "participant": 1,
      |   "creationTimestamp": "2021053123591",
      |   "actualStartDate": "20210406",
      |   "relationshipEndReason": "Ended by Participant 1",
      |   "participant1StartDate": "20210406",
      |   "participant2StartDate": "20210406",
      |   "actualEndDate": "20210406",
      |   "participant1EndDate": "20210406",
      |   "participant2EndDate": "20210406",
      |   "otherParticipantInstanceIdentifier": "123456789012345",
      |   "otherParticipantUpdateTimestamp": "20210531235901",
      |   "participant2UKResident": true
      |  }
      |   ]
      |}
      |
      |""".stripMargin)

  val createMultiYearRelationshipResponse = Json.parse("""
      |{
      |  "CID1": "999059794",
      |  "CID1Timestamp": "2021",
      |  "CID2": "999012345",
      |  "CID2Timestamp": "2021",
      |  "status" : "Processing OK"
      |}
      |""".stripMargin)

  val createMultiYearError = Json.parse(
    """
      |{
      |  "reason": "Participant 2 identified by Instance Identifier is merged out"
      |}
      |""".stripMargin
  )

  val LTM000503Error = Json.parse(
    """
      |{
      |  "incidentReference":"LTM000503"
      |}
      |""".stripMargin
  )

  val unableToUpdateError = Json.parse(
    """
      |{
      |  "Reason": "Cannot update as Participant 1, update timestamp has changed since last view of data"
      |}
      |""".stripMargin
  )

  val updateAllowanceRelationshipResponse = Json.parse(
    """
      |{
      |  "participant1": {
      |    "updateTimestamp": "20210531235901",
      |    "endDate": "20101230"
      |  },
      |  "participant2": {
      |    "updateTimestamp": "20210531235901",
      |    "endDate": "20101230"
      |  },
      |  "relationship": {
      |    "actualEndDate": "20101230"
      |  }
      |}
      |""".stripMargin
  )
}
