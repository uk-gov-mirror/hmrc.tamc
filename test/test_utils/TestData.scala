/*
 * Copyright 2026 HM Revenue & Customs
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

import models.*
import uk.gov.hmrc.domain.NinoGenerator

import java.net.URLDecoder
import scala.util.Random

object TestData {

  private def generateUnique[T](count: Int)(next: => T): List[T] =
    Iterator
      .continually(next)
      .distinct
      .take(count + 1)
      .toList

  object Cids {
    private lazy val cids = {
      val randomizer = new Random
      generateUnique(11)(randomizer.nextLong().abs)
    }

    val cidOkP1: Cid               = cids(0)
    val cidRecDoesNotExistP1: Cid  = cids(1)
    val cidOkP2: Cid               = cids(2)
    val cidRecDoesNotExistP2: Cid  = cids(3)
    val cidNonApi: Cid             = cids(4)
    val cidLong: Cid               = cids(5)
    val cidBadRequest: Cid         = cids(6)
    val cidCitizenNotFound: Cid    = cids(7)
    val cidServerError: Cid        = cids(8)
    val cidServiceUnavailable: Cid = cids(9)
    val cidConflict: Cid           = cids(10)
  }

  object Ninos {
    private lazy val ninos = {
      val randomizer = NinoGenerator()
      generateUnique(15)(randomizer.nextNino.nino)
    }

    val ninoP1A: String                = ninos(0)
    val ninoP2A: String                = ninos(1)
    val ninoP3A: String                = ninos(2)
    val ninoP4A: String                = ninos(3)
    val ninoP5A: String                = ninos(4)
    val ninoP6A: String                = ninos(5)
    val ninoP7A: String                = ninos(6)
    val ninoP1C: String                = ninos(7)
    val ninoP3C: String                = ninos(8)
    val ninoP5C: String                = ninos(9)
    val ninoDeceased: String           = ninos(10)
    val ninoTransferorNotFound: String = ninos(11)
    val ninoBadRequest: String         = ninos(12)
    val ninoCitizenNotFound: String    = ninos(13)
    val ninoServerError: String        = ninos(14)
    val ninoServiceUnavailable: String = ninos(15)
  }

  case class ListItem(partner: FindCitizenDummy, participant: Int, endReason: Option[String] = None) {
    def json: String = endReason match {
      case Some(reason) => ended(reason)
      case None         => ongoing
    }

    private def ended(reason: String) = s"""
{
      "participant": $participant,
      "creationTimestamp": "20150531235901",
      "actualStartDate": "20011230",
      "relationshipEndReason": "$reason",
      "participant1StartDate": "20011230",
      "participant2StartDate": "20011230",
      "actualEndDate": "20101230",
      "participant1EndDate": "20101230",
      "participant2EndDate": "20101230",
      "otherParticipantInstanceIdentifier": "${partner.cid.cid}",
      "otherParticipantUpdateTimestamp": "${partner.timestamp}",
      "participant2UKResident": true
    }"""

    private def ongoing = s"""
{
      "participant": 1,
      "creationTimestamp": "20150531235901",
      "actualStartDate": "20011230",
      "participant1StartDate": "20011230",
      "participant2StartDate": "20011230",
      "otherParticipantInstanceIdentifier": "${partner.cid.cid}",
      "otherParticipantUpdateTimestamp": "${partner.timestamp}",
      "participant2UKResident": true
    }"""

  }

  case class ListDummy(user: FindCitizenDummy, counterparties: Array[ListItem]) {
    def key = s"usercid-${user.cid.cid}"

    def json: String = counterparties.map(_.json).mkString("""{"relationships": [""", ",", "]}")
  }

  object Lists {
    val noRelations          = ListDummy(mappedNino2FindCitizen(Ninos.ninoP2A), Array[ListItem]())
    val noRelationsAlt       = ListDummy(mappedNino2FindCitizen(Ninos.ninoP4A), Array[ListItem]())
    val oneActiveOneHistoric = ListDummy(
      mappedNino2FindCitizen(Ninos.ninoP1A),
      Array[ListItem](
        ListItem(mappedNino2FindCitizen(Ninos.ninoP2A), 1),
        ListItem(mappedNino2FindCitizen(Ninos.ninoP4A), 1, Some("Death (either participant)"))
      )
    )
    val oneHistoric          = ListDummy(
      mappedNino2FindCitizen(Ninos.ninoP3A),
      Array[ListItem](ListItem(mappedNino2FindCitizen(Ninos.ninoP2A), 1, Some("Ended by Participant 2")))
    )
    val oneActive            = ListDummy(
      mappedNino2FindCitizen(Ninos.ninoP5A),
      Array[ListItem](ListItem(mappedNino2FindCitizen(Ninos.ninoP2A), 1))
    )
    val deceased             = ListDummy(
      mappedNino2FindCitizen(Ninos.ninoDeceased),
      Array[ListItem](ListItem(mappedNino2FindCitizen(Ninos.ninoP2A), 1, Some("Ended by Participant 2")))
    )
    val tansfrorNotFound     = ListDummy(
      mappedNino2FindCitizen(Ninos.ninoTransferorNotFound),
      Array[ListItem](ListItem(mappedNino2FindCitizen(Ninos.ninoP2A), 1, Some("Ended by Participant 2")))
    )
    val badRequest           = ListDummy(mappedNino2FindCitizen(Ninos.ninoBadRequest), Array[ListItem]())
    val citizenNotFound      = ListDummy(mappedNino2FindCitizen(Ninos.ninoCitizenNotFound), Array[ListItem]())
    val serverError          = ListDummy(mappedNino2FindCitizen(Ninos.ninoServerError), Array[ListItem]())
    val serviceUnavailable   = ListDummy(mappedNino2FindCitizen(Ninos.ninoServiceUnavailable), Array[ListItem]())
  }

  lazy val mappedLists =
    Map(
      Lists.noRelations.key          -> Lists.noRelations,
      Lists.oneActiveOneHistoric.key -> Lists.oneActiveOneHistoric,
      Lists.oneHistoric.key          -> Lists.oneHistoric,
      Lists.oneActive.key            -> Lists.oneActive,
      Lists.noRelationsAlt.key       -> Lists.noRelationsAlt,
      Lists.deceased.key             -> Lists.deceased,
      Lists.tansfrorNotFound.key     -> Lists.tansfrorNotFound
    )

  case class CreateRelationshipDummy(
    returnCode: Int,
    reasonCode: Int,
    transferor: FindCitizenDummy,
    recipient: FindCitizenDummy
  ) {
    def key =
      s"trcid-${transferor.cid.cid}_trts-${transferor.timestamp}_rccid-${recipient.cid.cid}_rcts-${recipient.timestamp}"

    def json = s"""
{
    "Jtpr1481PerRelCreatecallResponse":{
        "Jtpr1481PerRelCreateExport":{
            "OutWCbdParameters":{
                "ReturnCode":$returnCode,
                "ReasonCode":$reasonCode
            }
        }
    }
}"""
  }

  case class UpdateRelationshipDummy(transferor: FindCitizenDummy, recipient: FindCitizenDummy, reason: String) {
    def key = s"cid1-${recipient.cid.cid}_part2ts-${transferor.timestamp}_endReason-$reason"

    def json = s"""
{
  "participant1": {
    "updateTimestamp": "20150531235901",
    "endDate": "20101230"
  },
  "participant2": {
    "updateTimestamp": "20150531235901",
    "endDate": "20101230"
  },
  "relationship": {
    "actualEndDate": "20101230"
  }
}"""
  }

  object Updates {
    val cancel: UpdateRelationshipDummy     = UpdateRelationshipDummy(
      mappedNino2FindCitizen(Ninos.ninoP2A),
      mappedNino2FindCitizen(Ninos.ninoP4A),
      "Cancelled by Transferor"
    )
    val reject: UpdateRelationshipDummy     = UpdateRelationshipDummy(
      mappedNino2FindCitizen(Ninos.ninoP2A),
      mappedNino2FindCitizen(Ninos.ninoP4A),
      "Rejected by Recipient"
    )
    val divorceRec: UpdateRelationshipDummy = UpdateRelationshipDummy(
      mappedNino2FindCitizen(Ninos.ninoP2A),
      mappedNino2FindCitizen(Ninos.ninoP4A),
      "Divorce/Separation"
    )
    val divorceTr: UpdateRelationshipDummy  = UpdateRelationshipDummy(
      mappedNino2FindCitizen(Ninos.ninoP2A),
      mappedNino2FindCitizen(Ninos.ninoP4A),
      "Divorce/Separation"
    )

    val badRequest: UpdateRelationshipDummy         = UpdateRelationshipDummy(
      mappedNino2FindCitizen(Ninos.ninoBadRequest),
      mappedNino2FindCitizen(Ninos.ninoBadRequest),
      "Cancelled by Transferor"
    )
    val citizenNotFound: UpdateRelationshipDummy    = UpdateRelationshipDummy(
      mappedNino2FindCitizen(Ninos.ninoCitizenNotFound),
      mappedNino2FindCitizen(Ninos.ninoCitizenNotFound),
      "Cancelled by Transferor"
    )
    val serverError: UpdateRelationshipDummy        = UpdateRelationshipDummy(
      mappedNino2FindCitizen(Ninos.ninoServerError),
      mappedNino2FindCitizen(Ninos.ninoServerError),
      "Cancelled by Transferor"
    )
    val serviceUnavailable: UpdateRelationshipDummy = UpdateRelationshipDummy(
      mappedNino2FindCitizen(Ninos.ninoServiceUnavailable),
      mappedNino2FindCitizen(Ninos.ninoServiceUnavailable),
      "Cancelled by Transferor"
    )
  }

  lazy val mappedUpdates =
    Map(
      Updates.cancel.key             -> Updates.cancel,
      Updates.reject.key             -> Updates.reject,
      Updates.divorceRec.key         -> Updates.divorceRec,
      Updates.divorceTr.key          -> Updates.divorceTr,
      Updates.badRequest.key         -> Updates.badRequest,
      Updates.citizenNotFound.key    -> Updates.citizenNotFound,
      Updates.serverError.key        -> Updates.serverError,
      Updates.serviceUnavailable.key -> Updates.serviceUnavailable
    )

  object Creations {
    val happyScenario: CreateRelationshipDummy =
      CreateRelationshipDummy(1, 1, mappedNino2FindCitizen(Ninos.ninoP2A), mappedNino2FindCitizen(Ninos.ninoP4A))
    val relExists: CreateRelationshipDummy     =
      CreateRelationshipDummy(1, 2, mappedNino2FindCitizen(Ninos.ninoP2A), mappedNino2FindCitizen(Ninos.ninoP3A))
  }

  lazy val mappedCreations =
    Map(Creations.happyScenario.key -> Creations.happyScenario, Creations.relExists.key -> Creations.relExists)

  object Recipients {
    val recHasAllowance: FindRecipientDummy               = FindRecipientDummy(
      returnCode = 1,
      reasonCode = 1,
      citizen = mappedNino2FindCitizen(Ninos.ninoP1A),
      firstName = "rty",
      lastName = "qwe",
      gender = "M"
    )
    val recHasAllowanceAndSpaceInName: FindRecipientDummy = FindRecipientDummy(
      returnCode = 1,
      reasonCode = 1,
      citizen = mappedNino2FindCitizen(Ninos.ninoP1A),
      firstName = "rty",
      lastName = "qwe abc",
      gender = "M"
    )
    val recHasNoAllowance: FindRecipientDummy             = FindRecipientDummy(
      returnCode = 1,
      reasonCode = 1,
      citizen = mappedNino2FindCitizen(Ninos.ninoP4A),
      firstName = "fgh",
      lastName = "asd",
      gender = "F"
    )
    val recHasNoAllowanceNoCid: FindRecipientDummy        = FindRecipientDummy(
      returnCode = 1,
      reasonCode = 2,
      citizen = mappedNino2FindCitizen(Ninos.ninoP1C),
      firstName = "foo",
      lastName = "bar",
      gender = "M"
    )
    val recCidErr: FindRecipientDummy                     = FindRecipientDummy(
      returnCode = 1,
      reasonCode = 1,
      citizen = mappedNino2FindCitizen(Ninos.ninoP3C),
      firstName = "foo",
      lastName = "bar",
      gender = "M"
    )
  }

  lazy val mappedFindRecipient = Map(
    Recipients.recHasAllowance.key               -> Recipients.recHasAllowance,
    Recipients.recHasAllowanceAndSpaceInName.key -> Recipients.recHasAllowanceAndSpaceInName,
    Recipients.recHasNoAllowance.key             -> Recipients.recHasNoAllowance,
    Recipients.recHasNoAllowanceNoCid.key        -> Recipients.recHasNoAllowanceNoCid,
    Recipients.recCidErr.key                     -> Recipients.recCidErr
  )

  case class FindRecipientDummy(
    returnCode: Int,
    reasonCode: Int,
    citizen: FindCitizenDummy,
    firstName: String,
    lastName: String,
    gender: String,
    showCid: Boolean = true
  ) {

    def decodeQueryStringValue(value: String) = URLDecoder.decode(value, "UTF-8")

    def key = s"/data/findRecipient/nino-${citizen.nino}.json"

    def json = s"""
{
    "Jfwk1012FindCheckPerNoninocallResponse":{
        "Jfwk1012FindCheckPerNoninoExport":{
            "OutItpr1Person":{
                "InstanceIdentifier":${citizen.cid.cid},
                "UpdateTimestamp":"${citizen.timestamp}"
            },
            "OutWCbdParameters":{
                "ReturnCode":$returnCode,
                "ReasonCode":$reasonCode
            }
        }
    }
}"""
  }

  lazy val mappedNino2FindCitizen = Map(
    Ninos.ninoP1C                -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 2,
      cid = CheckAllowanceRelationshipDummy(0, 0, 0),
      timestamp = "999000999",
      firstName = "",
      lastName = "",
      deceased = "Y",
      nino = Ninos.ninoP1C
    ),
    Ninos.ninoP3C                -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidNonApi.toString()),
      timestamp = "888999888",
      firstName = "Firstnameone",
      lastName = "Lastnameone",
      deceased = "N",
      nino = Ninos.ninoP3C
    ),
    Ninos.ninoP5C                -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidLong.toString()),
      timestamp = "777888777",
      firstName = "Firstnametwo",
      lastName = "Lastnametwo",
      deceased = "N",
      nino = Ninos.ninoP5C
    ),
    Ninos.ninoP1A                -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidOkP1.toString()),
      timestamp = "666777666",
      firstName = "Firstnamethree",
      lastName = "Lastnamethree",
      deceased = "N",
      nino = Ninos.ninoP1A
    ),
    Ninos.ninoP2A                -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidRecDoesNotExistP1.toString),
      timestamp = "555444555",
      firstName = "Firstnamefour",
      lastName = "Lastnamefour",
      deceased = "N",
      nino = Ninos.ninoP2A
    ),
    Ninos.ninoP3A                -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidOkP2.toString),
      timestamp = "333222333",
      firstName = "Firstnamefourz",
      lastName = "Lastnamefourz",
      deceased = "N",
      nino = Ninos.ninoP3A
    ),
    Ninos.ninoP4A                -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidRecDoesNotExistP2.toString),
      timestamp = "222111222",
      firstName = "FirstnameSeven",
      lastName = "LastnameSeven",
      deceased = "N",
      nino = Ninos.ninoP4A
    ),
    Ninos.ninoP5A                -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidLong.toString()),
      timestamp = "12121212121212121212121212121212",
      firstName = "Firstnamefivefivefivefivefivefivefivefivefive",
      lastName = "Lastnamefivefivefivefivefivefivefivefivefive",
      deceased = "N",
      nino = Ninos.ninoP5A
    ),
    Ninos.ninoP6A                -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidOkP1.toString()),
      timestamp = "4545454545454545454545",
      firstName = "Firstnamesix",
      lastName = "Lastnamesix",
      deceased = "Y",
      nino = Ninos.ninoP6A
    ),
    Ninos.ninoP7A                -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidOkP1.toString()),
      timestamp = "90909090909090909090909090909090",
      firstName = null,
      lastName = null,
      deceased = "N",
      nino = Ninos.ninoP7A
    ),
    Ninos.ninoDeceased           -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidOkP2.toString),
      timestamp = "333222333",
      firstName = "Firstnamefourz",
      lastName = "Lastnamefourz",
      deceased = "Y",
      nino = Ninos.ninoDeceased
    ),
    Ninos.ninoTransferorNotFound -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 3,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidOkP2.toString),
      timestamp = "333222333",
      firstName = "Firstnamefourz",
      lastName = "Lastnamefourz",
      deceased = "N",
      nino = Ninos.ninoTransferorNotFound
    ),
    Ninos.ninoBadRequest         -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidBadRequest.toString),
      timestamp = "333222333",
      firstName = "Firstnamefourz",
      lastName = "Lastnamefourz",
      deceased = "N",
      nino = Ninos.ninoBadRequest
    ),
    Ninos.ninoCitizenNotFound    -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidCitizenNotFound.toString),
      timestamp = "333222333",
      firstName = "Firstnamefourz",
      lastName = "Lastnamefourz",
      deceased = "N",
      nino = Ninos.ninoCitizenNotFound
    ),
    Ninos.ninoServerError        -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidServerError.toString),
      timestamp = "333222333",
      firstName = "Firstnamefourz",
      lastName = "Lastnamefourz",
      deceased = "N",
      nino = Ninos.ninoServerError
    ),
    Ninos.ninoServiceUnavailable -> FindCitizenDummy(
      returnCode = 1,
      reasonCode = 1,
      cid = mappedCid2CheckAllowanceRelationship(Cids.cidServiceUnavailable.toString),
      timestamp = "333222333",
      firstName = "Firstnamefourz",
      lastName = "Lastnamefourz",
      deceased = "N",
      nino = Ninos.ninoServiceUnavailable
    )
  )

  lazy val mappedCid2CheckAllowanceRelationship = Map(
    Cids.cidOkP1.toString()   -> CheckAllowanceRelationshipDummy(returnCode = 1, reasonCode = 1, cid = Cids.cidOkP1),
    Cids.cidRecDoesNotExistP1
      .toString()             -> CheckAllowanceRelationshipDummy(returnCode = 1, reasonCode = 3, cid = Cids.cidRecDoesNotExistP1),
    Cids.cidOkP2.toString()   -> CheckAllowanceRelationshipDummy(returnCode = 1, reasonCode = 1, cid = Cids.cidOkP2),
    Cids.cidRecDoesNotExistP2
      .toString()             -> CheckAllowanceRelationshipDummy(returnCode = 1, reasonCode = 3, cid = Cids.cidRecDoesNotExistP2),
    Cids.cidNonApi.toString() -> CheckAllowanceRelationshipDummy(returnCode = 1, reasonCode = 2, cid = Cids.cidNonApi),
    Cids.cidLong.toString()   -> CheckAllowanceRelationshipDummy(returnCode = 1, reasonCode = 1, cid = Cids.cidLong),
    Cids.cidBadRequest
      .toString()             -> CheckAllowanceRelationshipDummy(returnCode = 1, reasonCode = 1, cid = Cids.cidBadRequest),
    Cids.cidCitizenNotFound
      .toString()             -> CheckAllowanceRelationshipDummy(returnCode = 1, reasonCode = 1, cid = Cids.cidCitizenNotFound),
    Cids.cidServerError
      .toString()             -> CheckAllowanceRelationshipDummy(returnCode = 1, reasonCode = 1, cid = Cids.cidServerError),
    Cids.cidServiceUnavailable
      .toString()             -> CheckAllowanceRelationshipDummy(returnCode = 1, reasonCode = 1, cid = Cids.cidServiceUnavailable)
  )

  case class CheckAllowanceRelationshipDummy(returnCode: Int, reasonCode: Int, cid: Cid) {
    def json = s"""
{
    "Jtpr1491PerRelCheckcallResponse":{
        "Jtpr1491PerRelCheckExport":{
            "OutWCbdParameters":{
                "ReturnCode":$returnCode,
                "ReasonCode":$reasonCode
            }
        }
    }
}"""
  }

  case class FindCitizenDummy(
    returnCode: Int,
    reasonCode: Int,
    cid: CheckAllowanceRelationshipDummy,
    timestamp: Timestamp,
    firstName: String,
    lastName: String,
    deceased: String,
    nino: String
  ) {

    def nulify(value: String) = value match {
      case null => "null"
      case x    => "\"" + x + "\""
    }

    def json = s"""
{
    "Jtpr1311PerDetailsFindcallResponse":{
        "Jtpr1311PerDetailsFindExport":{
            "OutItpr1Person":{
                "InstanceIdentifier":${cid.cid},
                "UpdateTimestamp":${nulify(timestamp)},
                "Surname":${nulify(lastName)},
                "FirstForename":${nulify(firstName)},
                "DeceasedSignal":"$deceased"
            },
            "OutWCbdParameters":{
                "ReturnCode":$returnCode,
                "ReasonCode":$reasonCode
            }
        }
    }
}"""
  }

  object MultiYearCreate {
    val happyScenarioStep1 = CreateAllowanceTransferRelationshipResponse(
      status = "Processing OK",
      transferor = mappedNino2FindCitizen(Ninos.ninoP2A).copy(timestamp = "100000001"),
      recipient = mappedNino2FindCitizen(Ninos.ninoP4A).copy(timestamp = "200000001"),
      transferorOutTimestamp = Some("100000002"),
      recipientOutTimestamp = Some("200000002")
    )
    val happyScenarioStep2 = CreateAllowanceTransferRelationshipResponse(
      status = "Processing OK",
      transferor = mappedNino2FindCitizen(Ninos.ninoP2A).copy(timestamp = "100000002"),
      recipient = mappedNino2FindCitizen(Ninos.ninoP4A).copy(timestamp = "200000002"),
      transferorOutTimestamp = Some("100000003"),
      recipientOutTimestamp = Some("200000003")
    )
  }

  lazy val mappedMultiYearCreate = Map(
    MultiYearCreate.happyScenarioStep1.key -> MultiYearCreate.happyScenarioStep1,
    MultiYearCreate.happyScenarioStep2.key -> MultiYearCreate.happyScenarioStep2
  )

  case class CreateAllowanceTransferRelationshipResponse(
    status: String,
    transferor: FindCitizenDummy,
    recipient: FindCitizenDummy,
    transferorOutTimestamp: Option[Timestamp] = None,
    recipientOutTimestamp: Option[Timestamp] = None
  ) {

    def key =
      s"trcid-${transferor.cid.cid}_trts-${transferor.timestamp}_rccid-${recipient.cid.cid}_rcts-${recipient.timestamp}"

    def json = (transferorOutTimestamp, recipientOutTimestamp) match {
      case (Some(transferorTs), Some(recipientTs)) => s"""
{
  "CID1": "${recipient.cid.cid}",
  "CID1Timestamp": "$recipientTs",
  "CID2": "${transferor.cid.cid}",
  "CID2Timestamp": "$transferorTs",
  "status" : "$status"
}"""
      case _                                       => s"""
{
  "status" : "$status"
}"""
    }

  }

  def findMockData[T](url: String, body: Option[T] = None): String = {

    val findCitizenByNinoUrl                    =
      """^GET-foo/marriage-allowance/citizen/((?!BG|GB|NK|KN|TN|NT|ZZ)[ABCEGHJ-PRSTW-Z][ABCEGHJ-NPRSTW-Z]\d{6}[A-D]$)""".r
    val findRecipientByNinoUrl                  =
      """^POST-foo/marriage-allowance/citizen/((?!BG|GB|NK|KN|TN|NT|ZZ)[ABCEGHJ-PRSTW-Z][ABCEGHJ-NPRSTW-Z]\d{6}[A-D])/check""".r
    val checkAllowanceRelationshipUrl           = """^GET-foo/marriage-allowance/citizen/(\d+)/relationship""".r
    val listRelationshipUrl                     = """^GET-foo/marriage-allowance/citizen/(\d+)/relationships\?includeHistoric=true""".r
    val createAllowanceRelationshipUrl          = """^POST-foo/marriage-allowance/citizen/(\d+)/relationship""".r
    val multiYearCreateAllowanceRelationshipUrl =
      """^POST-foo/marriage-allowance/02.00.00/citizen/(\d+)/relationship/([a-zA-Z]+)""".r
    val updateAllowanceRelationshipUrl          = """^PUT-foo/marriage-allowance/citizen/(\d+)/relationship""".r

    (url, body) match {
      case (checkAllowanceRelationshipUrl(cid), None)                                               =>
        TestData.mappedCid2CheckAllowanceRelationship(cid).json
      case (createAllowanceRelationshipUrl(recipientCid), Some(body: DesCreateRelationshipRequest)) =>
        val bodyToText = s"trcid-${body.CID2}_trts-${body.CID2Timestamp}_rccid-${body.CID1}_rcts-${body.CID1Timestamp}"
        TestData.mappedCreations(bodyToText).json
      case (
            multiYearCreateAllowanceRelationshipUrl(recipientCid, reqType: String),
            Some(body: MultiYearDesCreateRelationshipRequest)
          ) =>
        val bodyToText =
          s"trcid-${body.transferorCid}_trts-${body.transferorTimestamp}_rccid-${body.recipientCid}_rcts-${body.recipientTimestamp}"
        TestData.mappedMultiYearCreate(bodyToText).json
      case (findCitizenByNinoUrl(nino), None)                                                       =>
        TestData.mappedNino2FindCitizen(nino).json
      case (findRecipientByNinoUrl(nino), Some(_))                                                  =>
        val filePath = s"/data/findRecipient/nino-$nino.json"
        TestData.mappedFindRecipient(filePath).json
      case (listRelationshipUrl(cid), None)                                                         =>
        val filePath = s"usercid-$cid"
        TestData.mappedLists(filePath).json
      case (updateAllowanceRelationshipUrl(recipientCid), Some(body: DesUpdateRelationshipRequest)) =>
        val bodyToText =
          s"cid1-${body.participant1.instanceIdentifier}_part2ts-${body.participant2.updateTimestamp}_endReason-${body.relationship.relationshipEndReason}"
        TestData.mappedUpdates(bodyToText).json
      case _                                                                                        =>
        throw new IllegalArgumentException("url not supported:" + url)
    }
  }

  def decodeQueryStringValue(value: String) =
    URLDecoder.decode(value, "UTF-8")

}
