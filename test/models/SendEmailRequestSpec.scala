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

package models

import models.emailAddress.EmailAddress
import play.api.libs.json.{JsObject, Json}
import test_utils.UnitSpec

class SendEmailRequestSpec extends UnitSpec {

  private val sendEmailRequest: SendEmailRequest =
    SendEmailRequest(
      to = List(new EmailAddress("bob@test.com")),
      templateId = "tamc_recipient_rejects_retro_yr",
      parameters = Map("a" -> "b"),
      force = false
    )

  private val jsonObj: JsObject =
    Json.obj(
      "to"         -> Json.arr("bob@test.com"),
      "templateId" -> "tamc_recipient_rejects_retro_yr",
      "parameters" -> Json.obj("a" -> "b"),
      "force"      -> false
    )

  "SendEmailRequest" should {
    "serialise and de-serialise" in {
      Json.toJson(sendEmailRequest) shouldBe jsonObj

      jsonObj.as[SendEmailRequest].to         shouldBe List(new EmailAddress("bob@test.com"))
      jsonObj.as[SendEmailRequest].templateId shouldBe "tamc_recipient_rejects_retro_yr"
      jsonObj.as[SendEmailRequest].parameters shouldBe Map("a" -> "b")
      jsonObj.as[SendEmailRequest].force      shouldBe false
    }
  }
}
