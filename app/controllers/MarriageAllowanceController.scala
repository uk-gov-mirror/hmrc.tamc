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

package controllers

import com.google.inject.Inject
import controllers.auth.PertaxAuthAction
import errors.*
import errors.ErrorResponseStatus.*
import models.*
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import services.MarriageAllowanceService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import uk.gov.hmrc.http.{BadRequestException, NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

class MarriageAllowanceController @Inject() (
  marriageAllowanceService: MarriageAllowanceService,
  pertaxAuthAction: PertaxAuthAction,
  cc: ControllerComponents
)(implicit
  ec: ExecutionContext
) extends BackendController(cc)
    with Logging {

  def getRecipientRelationship(transferorNino: Nino): Action[JsValue] =
    pertaxAuthAction.async(parse.json) { implicit request =>
      withJsonBody[FindRecipientRequest] { findRecipientRequest =>
        marriageAllowanceService.getRecipientRelationship(transferorNino, findRecipientRequest) map {
          case Right((recipientRecord, taxYears)) =>
            Ok(
              Json.toJson(
                GetRelationshipResponse(
                  user_record = Some(recipientRecord),
                  availableYears = Some(taxYears),
                  status = ResponseStatus(status_code = "OK")
                )
              )
            )
          case Left(_: DataRetrievalError)        =>
            NotFound(Json.toJson(GetRelationshipResponse(status = ResponseStatus(status_code = RECIPIENT_NOT_FOUND))))
        } recover {
          case error: ServiceError            =>
            logger.warn(error.getMessage)
            NotFound(Json.toJson(GetRelationshipResponse(status = ResponseStatus(status_code = RECIPIENT_NOT_FOUND))))
          case error: TransferorDeceasedError =>
            logger.warn(error.getMessage)
            BadRequest(Json.toJson(GetRelationshipResponse(status = ResponseStatus(status_code = TRANSFERER_DECEASED))))
          case error                          =>
            logger.error(error.getMessage)
            InternalServerError(
              Json.toJson(GetRelationshipResponse(status = ResponseStatus(status_code = OTHER_ERROR)))
            )
        }
      }
    }

  def createMultiYearRelationship(transferorNino: Nino, journey: String): Action[JsValue] =
    pertaxAuthAction.async(parse.json) { implicit request =>
      withJsonBody[MultiYearCreateRelationshipRequestHolder] { createRelationshipRequestHolder =>
        marriageAllowanceService.createMultiYearRelationship(createRelationshipRequestHolder, journey) map { _ =>
          Ok(Json.toJson(CreateRelationshipResponse(status = ResponseStatus(status_code = "OK"))))
        } recover {
          case badRequest: BadRequestException if badRequest.message.contains("Participant is deceased")    =>
            logger.warn(badRequest.getMessage)
            BadRequest(
              Json.toJson(CreateRelationshipResponse(status = ResponseStatus(status_code = RECIPIENT_DECEASED)))
            )
          case conflict: UpstreamErrorResponse if conflict.message.contains("Cannot update as Participant") =>
            logger.warn(conflict.getMessage)
            Conflict(
              Json.toJson(CreateRelationshipResponse(status = ResponseStatus(status_code = RELATION_MIGHT_BE_CREATED)))
            )
          case ex: UpstreamErrorResponse if ex.message.contains("LTM000503")                                =>
            logger.warn(ex.getMessage)
            Conflict(
              Json.toJson(CreateRelationshipResponse(status = ResponseStatus(status_code = RELATION_MIGHT_BE_CREATED)))
            )
          case ex                                                                                           =>
            logger.error(ex.getMessage)
            throw ex
        }
      }
    }

  def listRelationship(transferorNino: Nino): Action[AnyContent] =
    pertaxAuthAction.async { implicit request =>
      marriageAllowanceService.listRelationship(transferorNino) map { relationshipList =>
        Ok(
          Json.toJson(
            RelationshipRecordStatusWrapper(
              relationship_record = relationshipList,
              status = ResponseStatus(status_code = "OK")
            )
          )
        )
      } recover { case error =>
        error match {
          case _: TransferorDeceasedError                                      =>
            logger.warn(error.getMessage)
            NotFound(
              Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = TRANSFEROR_NOT_FOUND)))
            )
          case _: ServiceError                                                 =>
            logger.warn(error.getMessage)
            NotFound(
              Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = TRANSFEROR_NOT_FOUND)))
            )
          case error: UpstreamErrorResponse if error.statusCode == NOT_FOUND   =>
            logger.warn(error.getMessage)
            NotFound(
              Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = CITIZEN_NOT_FOUND)))
            )
          case _: NotFoundException                                            =>
            logger.warn(error.getMessage)
            NotFound(
              Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = CITIZEN_NOT_FOUND)))
            )
          case _: BadRequestException                                          =>
            logger.warn(error.getMessage)
            BadRequest(
              Json.toJson(
                RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = ErrorResponseStatus.BAD_REQUEST))
              )
            )
          case error: UpstreamErrorResponse if error.statusCode == BAD_REQUEST =>
            logger.warn(error.getMessage)
            BadRequest(
              Json.toJson(
                RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = ErrorResponseStatus.BAD_REQUEST))
              )
            )
          case WithStatusCode(INTERNAL_SERVER_ERROR)                           =>
            logger.warn(error.getMessage)
            InternalServerError(
              Json.toJson(RelationshipRecordStatusWrapper(status = ResponseStatus(status_code = SERVER_ERROR)))
            )
          case WithStatusCode(SERVICE_UNAVAILABLE)                             =>
            logger.warn(error.getMessage)
            ServiceUnavailable(
              Json.toJson(
                RelationshipRecordStatusWrapper(status =
                  ResponseStatus(status_code = ErrorResponseStatus.SERVICE_UNAVILABLE)
                )
              )
            )
          case otherError                                                      =>
            logger.error(error.getMessage)
            throw otherError
        }
      }
    }

  def updateRelationship(transferorNino: Nino): Action[JsValue] =
    pertaxAuthAction.async(parse.json) { implicit request =>
      withJsonBody[UpdateRelationshipRequestHolder] { updateRelationshipRequestHolder =>
        marriageAllowanceService.updateRelationship(updateRelationshipRequestHolder) map { _ =>
          Ok(Json.toJson(UpdateRelationshipResponse(status = ResponseStatus(status_code = "OK"))))
        } recover { case error =>
          error match {
            case _: RecipientDeceasedError  =>
              logger.warn(error.getMessage)
              BadRequest(
                Json.toJson(
                  UpdateRelationshipResponse(status = ResponseStatus(status_code = ErrorResponseStatus.BAD_REQUEST))
                )
              )
            case _: UpdateRelationshipError =>
              logger.warn(error.getMessage)
              BadRequest(
                Json.toJson(
                  UpdateRelationshipResponse(status = ResponseStatus(status_code = CANNOT_UPDATE_RELATIONSHIP))
                )
              )
            case otherError                 =>
              logger.error(error.getMessage)
              throw otherError
          }
        }
      }
    }
}
