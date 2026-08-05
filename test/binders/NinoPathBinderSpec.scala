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

package binders

import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, OptionValues}
import test_utils.UnitSpec
import uk.gov.hmrc.domain.Nino

class NinoPathBinderSpec extends UnitSpec with Matchers with OptionValues with EitherValues {

  private val pathBindable = NinoPathBinder.pathBindable

  private val validNino = Nino("AB123456C")

  "NinoPathBindable.pathBindable" should {

    "must bind valid Ninos" in {
      pathBindable.bind("key", validNino.nino).value shouldEqual validNino
    }

    "must not bind invalid Ninos" in {
      pathBindable.bind("key", "1234565ABC").isLeft shouldEqual true
    }

    "must unbind" in {
      pathBindable.unbind("key", validNino) shouldEqual validNino.nino
    }
  }
}
