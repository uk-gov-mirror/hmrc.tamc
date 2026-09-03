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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson, post, urlEqualTo}
import config.ApplicationConfig
import models._
import models.emailAddress.EmailAddress
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.RecoverMethods
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{BAD_GATEWAY, IM_A_TEAPOT}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.libs.json.Json
import play.api.{Application, inject}
import test_utils.UnitSpec
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailConnectorSpec
    extends UnitSpec
    with GuiceOneAppPerSuite
    with HttpClientV2Support
    with WireMockSupport
    with RecoverMethods {

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockAppConfig.EMAIL_URL).thenReturn(wireMockUrl)
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(Map("microservice.services.email.port" -> wireMockPort))
    .overrides(
      bind[HttpClientV2].toInstance(httpClientV2),
      bind[ApplicationConfig].toInstance(mockAppConfig)
    )
    .build()

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val connector: EmailConnector = new EmailConnector(httpClientV2, mockAppConfig)

  val addressList: List[EmailAddress] = List(new EmailAddress("bob@test.com"))
  val params: Map[String, String]     = Map("a" -> "b")
  val request: SendEmailRequest       =
    SendEmailRequest(addressList, "tamc_recipient_rejects_retro_yr", params, force = false)

  "sendEmail" should {
    "return Unit" when {
      "an email is sent" in {
        wireMockServer.stubFor(
          post(urlEqualTo("/hmrc/email"))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
            .willReturn(
              aResponse()
                .withStatus(200)
            )
        )

        val response = await(connector.sendEmail(request))
        response.toOption.get shouldBe a[Unit]
      }
    }

    "return Left and whatever the status is" when {
      "UpstreamErrorResponse returned" in {
        wireMockServer.stubFor(
          post(urlEqualTo("/hmrc/email"))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
            .willReturn(
              aResponse()
                .withStatus(500)
                .withBody("error")
            )
        )

        val response = await(connector.sendEmail(request))
        response.isLeft                       shouldBe true
        response.swap.toOption.get            shouldBe a[UpstreamErrorResponse]
        response.swap.toOption.get.statusCode shouldBe 500
      }
    }

    "return Left BAD_GATEWAY" when {
      "call fails" in {
        val mockHttp                       = mock[HttpClientV2]
        val requestBuilder: RequestBuilder = mock[RequestBuilder]

        when(mockHttp.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)

        val injector: Injector = GuiceApplicationBuilder()
          .overrides(
            inject.bind[HttpClientV2].toInstance(mockHttp),
            inject.bind[RequestBuilder].toInstance(requestBuilder)
          )
          .injector()

        val connector: EmailConnector = injector.instanceOf[EmailConnector]

        when(injector.instanceOf[HttpClientV2].post(any[URL])(any[HeaderCarrier]))
          .thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(using any, any, any))
          .thenReturn(injector.instanceOf[RequestBuilder])
        when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](using connector.reads, global))
          .thenReturn(Future.failed(new HttpException("broken", IM_A_TEAPOT)))

        val response = await(connector.sendEmail(request))
        response.isLeft               shouldBe true
        response.swap.toOption.get    shouldBe a[UpstreamErrorResponse]
        response.swap.toOption.get shouldEqual UpstreamErrorResponse("broken", BAD_GATEWAY)
      }
    }
  }
}
