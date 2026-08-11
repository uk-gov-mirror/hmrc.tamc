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

package config

import com.google.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.time.TaxYear

class ApplicationConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  import servicesConfig.{baseUrl, getConfString}

  val serviceUrl: String           = baseUrl("marriage-allowance-des")
  val pertaxUrl: String            = baseUrl("pertax")
  val urlHeaderEnvironment: String = getConfString("marriage-allowance-des.environment", "")
  val urlHeaderAuthorization       = s"Bearer ${getConfString("marriage-allowance-des.authorization-token", "")}"

  val EMAIL_URL: String = baseUrl("email")

  val EMAIL_UPDATE_CANCEL_TEMPLATE_ID                       = "tamc_update_cancel"
  val EMAIL_UPDATE_REJECT_TEMPLATE_ID                       = "tamc_update_reject"
  val EMAIL_UPDATE_DIVORCE_TRANSFEROR_BOY_TEMPLATE_ID       = "tamc_update_divorce_transferor_boy"
  val EMAIL_UPDATE_DIVORCE_RECIPIENT_EOY_TEMPLATE_ID        = "tamc_update_divorce_recipient_eoy"
  val EMAIL_TRANSFEROR_DIVORCE_PREVIOUR_YEAR                = "tamc_transferor_divorce_previous_yr"
  val EMAIL_RECIPIENT_DIVORCE_PREVIOUR_YEAR                 = "tamc_recipient_divorce_previous_yr"
  val EMAIL_RECIPIENT_REJECT_RETROSPECTIVE_YEAR             = "tamc_recipient_rejects_retro_yr"
  val EMAIL_APPLY_CURRENT_TAXYEAR_TEMPLATE_ID               = "tamc_current_year"
  val EMAIL_APPLY_RETROSPECTIVE_TAXYEAR_TEMPLATE_ID         = "tamc_retro_year"
  val EMAIL_APPLY_CURRENT_RETROSPECTIVE_TAXYEAR_TEMPLATE_ID = "tamc_current_retro_year"
  val EMAIL_TRANSFEROR_DIVORCE_CURRENT_YEAR                 = "tamc_transferor_divorce_current_yr"

  val EMAIL_UPDATE_CANCEL_WELSH_TEMPLATE_ID                       = "tamc_update_cancel_cy"
  val EMAIL_UPDATE_REJECT_WELSH_TEMPLATE_ID                       = "tamc_update_reject_cy"
  val EMAIL_UPDATE_DIVORCE_TRANSFEROR_BOY_WELSH_TEMPLATE_ID       = "tamc_update_divorce_transferor_boy_cy"
  val EMAIL_UPDATE_DIVORCE_RECIPIENT_EOY_WELSH_TEMPLATE_ID        = "tamc_update_divorce_recipient_eoy_cy"
  val EMAIL_TRANSFEROR_DIVORCE_PREVIOUR_YEAR_WELSH                = "tamc_transferor_divorce_previous_yr_cy"
  val EMAIL_RECIPIENT_DIVORCE_PREVIOUR_YEAR_WELSH                 = "tamc_recipient_divorce_previous_yr_cy"
  val EMAIL_RECIPIENT_REJECT_RETROSPECTIVE_YEAR_WELSH             = "tamc_recipient_rejects_retro_yr_cy"
  val EMAIL_APPLY_CURRENT_TAXYEAR_WELSH_TEMPLATE_ID               = "tamc_current_year_cy"
  val EMAIL_APPLY_RETROSPECTIVE_TAXYEAR_WELSH_TEMPLATE_ID         = "tamc_retro_year_cy"
  val EMAIL_APPLY_CURRENT_RETROSPECTIVE_TAXYEAR_WELSH_TEMPLATE_ID = "tamc_current_retro_year_cy"
  val EMAIL_TRANSFEROR_DIVORCE_CURRENT_YEAR_WELSH                 = "tamc_transferor_divorce_current_yr_cy"

  val ROLE_TRANSFEROR = "Transferor"
  val ROLE_RECIPIENT  = "Recipient"

  val REASON_CANCEL  = "Cancelled by Transferor"
  val REASON_REJECT  = "Rejected by Recipient"
  val REASON_DIVORCE = "Divorce/Separation"

  val START_DATE = "6 April "
  val END_DATE   = "5 April "

  val START_DATE_CY = "6 Ebrill"
  val END_DATE_CY   = "5 Ebrill"

  def currentTaxYear(): Int =
    configuration.getOptional[Int]("tamc-effective-tax-year").getOrElse(TaxYear.current.startYear)

  lazy val START_TAX_YEAR: Int           = configuration.getOptional[Int]("ma-start-tax-year").getOrElse(2015)
  lazy val MA_SUPPORTED_YEARS_COUNT: Int = configuration.getOptional[Int]("ma-supported-years-count").getOrElse(5)
}
