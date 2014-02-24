package services.ordnance_survey

import services.AddressLookupService
import models.domain.disposal_of_vehicle.AddressViewModel
import utils.helpers.Config
import play.api.Logger
import play.api.libs.ws.WS
import com.ning.http.client.Realm.AuthScheme
import services.ordnance_survey.domain.OSAddressbaseSearchResponse
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global

class AddressLookupServiceImpl extends AddressLookupService {
  val username = s"${Config.ordnanceSurveyUsername}"
  val password = s"${Config.ordnanceSurveyPassword}"
  val baseUrl = s"${Config.ordnanceSurveyBaseUrl}"

  // TODO extract common code out of the below methods
  override def fetchAddressesForPostcode(postcode: String): Future[Seq[(String, String)]] = {
    val resultPostcodeWithNoSpaces = postcodeWithNoSpaces(postcode)
    Logger.debug(s"Postcode (spaces removed) = ${resultPostcodeWithNoSpaces}")
    val endPoint = s"${baseUrl}/postcode?postcode=${resultPostcodeWithNoSpaces}&dataset=dpa" // TODO add lpi to URL, but need to set orgnaisation as Option on the type.
    Logger.debug(s"Calling Ordnance Survey postcode lookup service on ${endPoint}...")
    val futureOfResponse = WS.url(endPoint).withAuth(username = username, password = password, scheme = AuthScheme.BASIC).get()

    futureOfResponse.map { resp =>
        Logger.debug(s"Http response code from Ordnance Survey postcode lookup service was: ${resp.status}")
        val body = resp.json.as[OSAddressbaseSearchResponse]

        val results = if (body.results.isDefined)
          body.results.get.map { address => {
            address.DPA match {
              case Some(dpa) => (dpa.UPRN, dpa.address)
              case _ => ??? // TODO check if an LPI entry is present
            }
          }
          }
        else ??? // TODO handle no results

        results.sortBy(x => x._1) // Sort by UPRN. TODO check with BAs how they would want to sort a complex list such as for
    }.recoverWith {
      case e: Throwable => Future {
        Logger.error(s"Ordnance Survey postcode lookup service error: ${e}")
        Seq.empty
      }
    }
  }

  private def postcodeWithNoSpaces(postcode: String) = {
    postcode.filter(_ != ' ')
  }

  override def fetchAddressForUprn(uprn: String): Future[Option[AddressViewModel]] = {
    val endPoint = s"${baseUrl}/uprn?uprn=${uprn}&dataset=dpa" // TODO add lpi to URL, but need to set orgnaisation as Option on the type.
    Logger.debug(s"Calling Ordnance Survey uprn lookup service on ${endPoint}...")
    val futureOfResponse = WS.url(endPoint).withAuth(username = username, password = password, scheme = AuthScheme.BASIC).get()

    futureOfResponse.map { resp =>
        Logger.debug(s"Http response code from Ordnance Survey uprn lookup service was: ${resp.status}")
        val body = resp.json.as[OSAddressbaseSearchResponse]

        if (body.results.isDefined){
          val results = body.results.get.map { address =>
            address.DPA match {
              case Some(dpa) => AddressViewModel(uprn = Some(dpa.UPRN.toLong), address = dpa.address.split(","))
              case _ => ??? // TODO check if an LPI entry is present
            }
          }
          require(results.length >= 1, s"Should be at least one address for the UPRN: ${uprn}")
          Some(results(0))
        }
        else {
          Logger.error(s"No results returned by web service for submitted UPRN: ${uprn}")
          None
        }
    }.recoverWith {
      case e: Throwable => Future {
        Logger.error(s"Ordnance Survey uprn lookup service error: ${e}")
        ???
      }
    }
  }
}
