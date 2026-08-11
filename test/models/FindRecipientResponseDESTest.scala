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

import play.api.libs.json.Json
import test_utils.UnitSpec

class FindRecipientResponseDESTest extends UnitSpec {

  val reasonCode                 = 1
  val returnCode                 = 1
  val instanceIdentifier: Cid    = 123456789
  val updateTimestamp: Timestamp = "20200116155359011123"

  val expectedJson = Json.parse(s"""{
          "Jfwk1012FindCheckPerNoninocallResponse": {
            "Jfwk1012FindCheckPerNoninoExport": {
              "@exitStateType": "0",
              "@exitState": "0",
              "OutItpr1Person": {
                "InstanceIdentifier": $instanceIdentifier,
                "UpdateTimestamp": "$updateTimestamp"
              },
              "OutWCbdParameters": {
                "SeverityCode": "W",
                "DataStoreStatus": "S",
                "OriginServid": 9999,
                "ContextString": "ITPR1311_PER_DETAILS_FIND_S",
                "ReturnCode": $reasonCode,
                "ReasonCode": $returnCode,
                "Checksum": "a234jnjbhr9ui83"
              }
            }
          }
      }""")

  "FindRecipientResponseDES" should {

    "create a FindRecipientResponseDES given valid Json" in {

      val expectedModel = FindRecipientResponseDES(reasonCode, returnCode, instanceIdentifier, updateTimestamp)

      expectedJson.as[FindRecipientResponseDES] shouldBe expectedModel

    }
  }
}
