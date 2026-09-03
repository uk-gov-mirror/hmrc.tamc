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

import models.DesUpdateRelationshipRequest
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Headers
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.Authorization

case class HttpGETCallWithHeaders(
  url: String,
  env: Seq[(String, String)] = Seq(),
  bearerToken: Option[Authorization] = None
)
case class HttpPOSTCallWithHeaders(
  url: String,
  body: Any,
  env: Seq[(String, String)] = Seq(),
  bearerToken: Option[Authorization] = None
)
case class HttpPUTCallWithHeaders(
  url: String,
  body: DesUpdateRelationshipRequest,
  env: Seq[(String, String)] = Seq(),
  bearerToken: Option[Authorization] = None
)

class DummyHttpResponse(
  override val body: String,
  override val status: Int,
  val allHeaders: Map[String, Seq[String]] = Map.empty
) extends HttpResponse {
  override def json: JsValue = Json.parse(body)

  override def headers: Map[String, Seq[String]] = new Headers(_headers = Nil).toMap
}
