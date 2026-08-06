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

package controllers.auth

import cats.data.EitherT
import connectors.PertaxConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import test_utils.UnitSpec
import uk.gov.hmrc.http.UpstreamErrorResponse

import cats.instances.future._

import scala.concurrent.ExecutionContext.Implicits.global

class PertaxAuthActionSpec extends UnitSpec with GuiceOneAppPerSuite {

  class Harness(
    pertaxAuthAction: PertaxAuthAction
  ) extends InjectedController {
    def onPageLoad(): Action[AnyContent] =
      pertaxAuthAction { _ =>
        Ok("")
      }
  }

  private val pertaxConnector: PertaxConnector = mock[PertaxConnector]
  private val cc: ControllerComponents         = stubControllerComponents()

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(bind[PertaxConnector].toInstance(pertaxConnector))
    .build()

  "PertaxAuthAction" should {
    "return OK" when {
      "response is ACCESS_GRANTED" in {
        when(pertaxConnector.authorise(any(), any()))
          .thenReturn(EitherT.rightT(PertaxAuthResponse("ACCESS_GRANTED", "")))

        val authAction =
          new PertaxAuthAction(pertaxConnector, cc)
        val controller =
          new Harness(authAction)
        val result     =
          controller.onPageLoad()(FakeRequest())

        status(result) shouldBe OK
        verify(pertaxConnector, times(1)).authorise(any(), any())
      }
    }

    "return unauthorised" when {
      "response is not ACCESS_GRANTED" in {
        when(pertaxConnector.authorise(any(), any()))
          .thenReturn(EitherT.rightT(PertaxAuthResponse("ERROR_CODE", "")))

        val authAction = new PertaxAuthAction(pertaxConnector, cc)
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) shouldBe UNAUTHORIZED
      }
    }

    "return unauthorised" when {
      "response is UpstreamErrorResponse unauthorised" in {
        when(pertaxConnector.authorise(any(), any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

        val authAction = new PertaxAuthAction(pertaxConnector, cc)
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) shouldBe UNAUTHORIZED
      }
    }

    "return bad gateway" when {
      "response is UpstreamErrorResponse internal server error" in {
        when(pertaxConnector.authorise(any(), any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("Server error", INTERNAL_SERVER_ERROR)))

        val authAction = new PertaxAuthAction(pertaxConnector, cc)
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) shouldBe BAD_GATEWAY
      }
    }

    "return internal server error" when {
      "response is other UpstreamErrorResponse" in {
        when(pertaxConnector.authorise(any(), any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("ERROR_CODE", IM_A_TEAPOT)))

        val authAction = new PertaxAuthAction(pertaxConnector, cc)
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
