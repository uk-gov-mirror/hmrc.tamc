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

package metrics

import com.codahale.metrics.{Counter, MetricRegistry, Timer}
import com.codahale.metrics.Timer.Context
import com.google.inject.Inject
import models.ApiType
import models.ApiType.ApiType

class TamcMetrics @Inject() (registry: MetricRegistry) {

  val timers: Map[models.ApiType.Value, Timer] = Map(
    ApiType.FindCitizen        -> registry.timer("find-citizen-response-timer"),
    ApiType.FindRecipient      -> registry.timer("find-recipient-response-timer"),
    ApiType.CheckRelationship  -> registry.timer("check-relationship-response-timer"),
    ApiType.CreateRelationship -> registry.timer("create-relationship-response-timer"),
    ApiType.ListRelationship   -> registry.timer("list-relationship-response-timer"),
    ApiType.UpdateRelationship -> registry.timer("update-relationship-response-timer")
  )

  val successCounters: Map[models.ApiType.Value, Counter] = Map(
    ApiType.FindCitizen        -> registry.counter("find-citizen-success"),
    ApiType.FindRecipient      -> registry.counter("find-recipient-success"),
    ApiType.CheckRelationship  -> registry.counter("check-relationship-success"),
    ApiType.CreateRelationship -> registry.counter("create-relationship-success"),
    ApiType.ListRelationship   -> registry.counter("list-relationship-success"),
    ApiType.UpdateRelationship -> registry.counter("update-relationship-success")
  )

  val totalCounters: Map[models.ApiType.Value, Counter] = Map(
    ApiType.FindCitizen        -> registry.counter("find-citizen-total"),
    ApiType.FindRecipient      -> registry.counter("find-recipient-total"),
    ApiType.CheckRelationship  -> registry.counter("check-relationship-total"),
    ApiType.CreateRelationship -> registry.counter("create-relationship-total"),
    ApiType.ListRelationship   -> registry.counter("list-relationship-total"),
    ApiType.UpdateRelationship -> registry.counter("update-relationship-total")
  )

  val failureCounters: Map[models.ApiType.Value, Counter] = Map(
    ApiType.FindRecipient -> registry.counter("find-recipient-failure")
  )

  def startTimer(api: ApiType): Context = timers(api).time()

  def incrementSuccessCounter(api: ApiType): Unit = successCounters(api).inc()

  def incrementTotalCounter(api: ApiType): Unit = totalCounters(api).inc()

  def incrementFailedCounter(api: ApiType): Unit = failureCounters(api).inc()
}
