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

import models.RelationshipEndReason._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import test_utils.UnitSpec

class RelationshipEndReasonTest extends UnitSpec with GuiceOneAppPerSuite {

  "RelationshipEndReasonHodsReads" should {
    "read the HODS value for correct RelationshipEndReason" in {

      JsString("Death (either participant)").as[RelationshipEndReason](RelationshipEndReasonHodsReads) shouldBe Death
      JsString("Divorce/Separation").as[RelationshipEndReason](RelationshipEndReasonHodsReads)         shouldBe Divorce
      JsString("Relationship Type specific - for future use").as[RelationshipEndReason](
        RelationshipEndReasonHodsReads
      )                                                                                                shouldBe Default
      JsString("Invalid Participant").as[RelationshipEndReason](
        RelationshipEndReasonHodsReads
      )                                                                                                shouldBe InvalidParticipant
      JsString("Ended by Participant 2").as[RelationshipEndReason](RelationshipEndReasonHodsReads)     shouldBe Cancelled
      JsString("Ended by Participant 1").as[RelationshipEndReason](RelationshipEndReasonHodsReads)     shouldBe Rejected
      JsString("Ended by HMRC").as[RelationshipEndReason](RelationshipEndReasonHodsReads)              shouldBe Hmrc
      JsString("Closed by mutual consent of participants").as[RelationshipEndReason](
        RelationshipEndReasonHodsReads
      )                                                                                                shouldBe Closed
      JsString("Merger").as[RelationshipEndReason](RelationshipEndReasonHodsReads)                     shouldBe Merger
      JsString("Retrospective").as[RelationshipEndReason](RelationshipEndReasonHodsReads)              shouldBe Retrospective
      JsString("System Closure").as[RelationshipEndReason](RelationshipEndReasonHodsReads)             shouldBe System
      JsString("Active").as[RelationshipEndReason](RelationshipEndReasonHodsReads)                     shouldBe Active
    }
    "read Default if the reason is not recognised" in {
      JsString("dafdasfa").as[RelationshipEndReason](RelationshipEndReasonHodsReads)          shouldBe Default
      JsString("Some other reason").as[RelationshipEndReason](RelationshipEndReasonHodsReads) shouldBe Default
      JsString("Other").as[RelationshipEndReason](RelationshipEndReasonHodsReads)             shouldBe Default
    }
    "return JsResultException if the value is not string" in {
      a[JsResultException] shouldBe thrownBy(JsNull.as[RelationshipEndReason](RelationshipEndReasonHodsReads))
      a[JsResultException] shouldBe thrownBy(JsNumber(21).as[RelationshipEndReason](RelationshipEndReasonHodsReads))
    }
  }
  "writes"                         should {
    "write the value to the Json correctly" in {
      Json.toJson[RelationshipEndReason](Death)              shouldBe JsString(Death.value)
      Json.toJson[RelationshipEndReason](InvalidParticipant) shouldBe JsString(InvalidParticipant.value)
    }
  }
}
