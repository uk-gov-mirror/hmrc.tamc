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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, ok, post, urlEqualTo}
import controllers.auth.PertaxAuthResponse
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import test_utils.UnitSpec
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext

class PertaxConnectorSpec
    extends UnitSpec
    with GuiceOneAppPerSuite
    with Eventually
    with IntegrationPatience
    with ScalaFutures
    with WireMockSupport {

  wireMockServer.start()

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.pertax.port" -> wireMockServer.port())
    .build()

  lazy val pertaxConnector: PertaxConnector = app.injector.instanceOf[PertaxConnector]
  implicit lazy val ec: ExecutionContext    = app.injector.instanceOf[ExecutionContext]
  implicit val hc: HeaderCarrier            = HeaderCarrier()
  private val authoriseUrl: String          = s"/pertax/authorise"

  "PertaxAuthConnector" should {
    "return a PertaxAuthResponse with ACCESS_GRANTED code" in {
      wireMockServer.stubFor(
        post(urlEqualTo(authoriseUrl)).willReturn(
          ok(
            Json.prettyPrint(
              Json.obj(
                "code"    -> "ACCESS_GRANTED",
                "message" -> "Access granted"
              )
            )
          )
        )
      )

      val result = pertaxConnector.authorise.value.futureValue.getOrElse(PertaxAuthResponse("INCORRECT", "INCORRECT"))

      result shouldBe PertaxAuthResponse("ACCESS_GRANTED", "Access granted")
    }

    "return a PertaxAuthResponse with NO_HMRC_PT_ENROLMENT code with a redirect link" in {
      wireMockServer.stubFor(
        post(urlEqualTo(authoriseUrl)).willReturn(
          ok(
            Json.prettyPrint(
              Json.obj(
                "code"     -> "NO_HMRC_PT_ENROLMENT",
                "message"  -> "There is no valid HMRC PT enrolment",
                "redirect" -> "/tax-enrolment-assignment-frontend/account"
              )
            )
          )
        )
      )

      val result = pertaxConnector.authorise.value.futureValue.getOrElse(PertaxAuthResponse("INCORRECT", "INCORRECT"))

      result shouldBe PertaxAuthResponse("NO_HMRC_PT_ENROLMENT", "There is no valid HMRC PT enrolment")
    }

    "return a PertaxResponse with INVALID_AFFINITY code and an errorView" in {
      wireMockServer.stubFor(
        post(urlEqualTo(authoriseUrl)).willReturn(
          ok(
            Json.prettyPrint(
              Json.obj(
                "code"       -> "INVALID_AFFINITY",
                "message"    -> "The user is neither an individual or an organisation",
                "errorView"  -> "/path/for/partial",
                "statusCode" -> "401"
              )
            )
          )
        )
      )

      val result = pertaxConnector.authorise.value.futureValue.getOrElse(PertaxAuthResponse("INCORRECT", "INCORRECT"))

      result shouldBe PertaxAuthResponse("INVALID_AFFINITY", "The user is neither an individual or an organisation")
    }

    "return a PertaxResponse with MCI_RECORD code and an errorView" in {
      wireMockServer.stubFor(
        post(urlEqualTo(authoriseUrl)).willReturn(
          ok(
            Json.prettyPrint(
              Json.obj(
                "code"       -> "MCI_RECORD",
                "message"    -> "Manual correspondence indicator is set",
                "errorView"  -> "/path/for/partial",
                "statusCode" -> "423"
              )
            )
          )
        )
      )

      val result = pertaxConnector.authorise.value.futureValue.getOrElse(PertaxAuthResponse("INCORRECT", "INCORRECT"))

      result shouldBe PertaxAuthResponse("MCI_RECORD", "Manual correspondence indicator is set")
    }

    "return a UpstreamErrorResponse with the correct error code" in {
      List(
        BAD_REQUEST,
        NOT_FOUND,
        FORBIDDEN,
        INTERNAL_SERVER_ERROR
      ).foreach { error =>
        wireMockServer.stubFor(
          post(urlEqualTo(authoriseUrl)).willReturn(
            aResponse().withStatus(error)
          )
        )

        val result = pertaxConnector.authorise.value.futureValue.swap
          .getOrElse(UpstreamErrorResponse("INCORRECT", UNPROCESSABLE_ENTITY))

        result.statusCode shouldBe error
      }
    }
  }
}
