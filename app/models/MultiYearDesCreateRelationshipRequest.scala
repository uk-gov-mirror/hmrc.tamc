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

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

object MultiYearDesCreateRelationshipRequest {

  implicit val multiYearWrites: OWrites[MultiYearDesCreateRelationshipRequest] = (
    (__ \ "CID1").write[String] and
      (__ \ "CID1Timestamp").write[String] and
      (__ \ "CID2").write[String] and
      (__ \ "CID2Timestamp").write[String] and
      (__ \ "startDate").writeNullable[String] and
      (__ \ "endDate").writeNullable[String]
  )(o => Tuple.fromProductTyped(o))
}

case class MultiYearDesCreateRelationshipRequest(
  recipientCid: String,
  recipientTimestamp: String,
  transferorCid: String,
  transferorTimestamp: String,
  startDate: Option[String],
  endDate: Option[String]
)
