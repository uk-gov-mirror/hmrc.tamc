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

package Fixtures

import models._
import models.emailAddress.EmailAddress

import java.util.Calendar

object MultiYearCreateRelationshipRequestHolderFixture {

  val multiYearCreateRelationshipRequest: MultiYearCreateRelationshipRequest = MultiYearCreateRelationshipRequest(
    transferor_cid = 1111.asInstanceOf[Cid],
    transferor_timestamp = "2222",
    recipient_cid = 3333.asInstanceOf[Cid],
    recipient_timestamp = "4444",
    taxYears = List(2015, 2016)
  )

  val multiYearCreateRelationshipRequestNoTaxYear: MultiYearCreateRelationshipRequest =
    MultiYearCreateRelationshipRequest(
      transferor_cid = 1111.asInstanceOf[Cid],
      transferor_timestamp = "2222",
      recipient_cid = 3333.asInstanceOf[Cid],
      recipient_timestamp = "4444",
      taxYears = List()
    )

  def getCurrentYear: Int = Calendar.getInstance().get(Calendar.YEAR);

  val multiYearCreateRelationshipCurrentYearRequest: MultiYearCreateRelationshipRequest =
    MultiYearCreateRelationshipRequest(
      transferor_cid = 1111.asInstanceOf[Cid],
      transferor_timestamp = "2222",
      recipient_cid = 3333.asInstanceOf[Cid],
      recipient_timestamp = "4444",
      taxYears = List(getCurrentYear)
    )

  val createRelationshipNotificationRequest: CreateRelationshipNotificationRequest =
    CreateRelationshipNotificationRequest(
      full_name = "bob",
      email = EmailAddress("bob@yahoo.com"),
      welsh = false
    )

  val multiYearCreateRelationshipRequestHolder: MultiYearCreateRelationshipRequestHolder =
    MultiYearCreateRelationshipRequestHolder(multiYearCreateRelationshipRequest, createRelationshipNotificationRequest)

  val multiYearCreateRelationshipRequestNoTaxYearHolder: MultiYearCreateRelationshipRequestHolder =
    MultiYearCreateRelationshipRequestHolder(
      multiYearCreateRelationshipRequestNoTaxYear,
      createRelationshipNotificationRequest
    )

  val multiYearCreateRelationshipCurrentYearHolder: MultiYearCreateRelationshipRequestHolder =
    MultiYearCreateRelationshipRequestHolder(
      multiYearCreateRelationshipCurrentYearRequest,
      createRelationshipNotificationRequest
    )
}
