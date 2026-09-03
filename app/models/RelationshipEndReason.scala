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

import play.api.Logging
import play.api.libs.json._

sealed trait RelationshipEndReason {
  def value: String
}

object RelationshipEndReason {

  case object Death extends RelationshipEndReason {
    val value = "DEATH"
  }
  case object Divorce extends RelationshipEndReason {
    val value = "DIVORCE"
  }
  case object InvalidParticipant extends RelationshipEndReason {
    val value = "INVALID_PARTICIPANT"
  }
  case object Cancelled extends RelationshipEndReason {
    val value = "CANCELLED"
  }
  case object Rejected extends RelationshipEndReason {
    val value = "REJECTED"
  }
  case object Hmrc extends RelationshipEndReason {
    val value = "HMRC"
  }
  case object Closed extends RelationshipEndReason {
    val value = "CLOSED"
  }
  case object Merger extends RelationshipEndReason {
    val value = "MERGER"
  }
  case object Retrospective extends RelationshipEndReason {
    val value = "RETROSPECTIVE"
  }
  case object System extends RelationshipEndReason {
    val value = "SYSTEM"
  }
  case object Active extends RelationshipEndReason {
    val value = "Active"
  }
  case object Default extends RelationshipEndReason {
    val value = "DEFAULT"
  }

  implicit val writes: Writes[RelationshipEndReason] = new Writes[RelationshipEndReason] {
    override def writes(o: RelationshipEndReason): JsValue = JsString(o.value)
  }

  object RelationshipEndReasonHodsReads extends Reads[RelationshipEndReason] with Logging {
    override def reads(json: JsValue): JsResult[RelationshipEndReason] = json.as[String] match {
      case "Death (either participant)"                  => JsSuccess(Death)
      case "Divorce/Separation"                          => JsSuccess(Divorce)
      case "Relationship Type specific - for future use" => JsSuccess(Default)
      case "Invalid Participant"                         => JsSuccess(InvalidParticipant)
      case "Ended by Participant 2"                      => JsSuccess(Cancelled)
      case "Ended by Participant 1"                      => JsSuccess(Rejected)
      case "Ended by HMRC"                               => JsSuccess(Hmrc)
      case "Closed by mutual consent of participants"    => JsSuccess(Closed)
      case "Merger"                                      => JsSuccess(Merger)
      case "Retrospective"                               => JsSuccess(Retrospective)
      case "System Closure"                              => JsSuccess(System)
      case "Active"                                      => JsSuccess(Active)
      case unknown                                       =>
        logger.warn(s"Unexpected reason code :'$unknown'")
        JsSuccess(Default)
    }
  }
}
