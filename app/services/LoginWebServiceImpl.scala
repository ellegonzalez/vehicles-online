package services

import models.domain.change_of_address._
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import utils.helpers.Config
import models.domain.disposal_of_vehicle.AddressAndPostcodeModel
import models.domain.disposal_of_vehicle.AddressLinesModel

class LoginWebServiceImpl() extends LoginWebService {
  implicit val writeLoginPage = Json.writes[LoginPageModel]
  implicit val addressLinesModel = Json.reads[AddressLinesModel]
  implicit val addressAndPostcodeModel = Json.reads[AddressAndPostcodeModel]
  implicit val loginConfirmationModel = Json.reads[LoginConfirmationModel]
  implicit val loginResponse = Json.reads[LoginResponse]

  override def invoke(cmd: LoginPageModel): Future[LoginResponse] = {
    val endPoint = s"${Config.microServiceUrlBase}/login-page"
    Logger.debug(s"Calling Login micro service on ${endPoint}...")
    val futureOfResponse = WS.url(endPoint).post(Json.toJson(cmd))

    futureOfResponse.map {
      resp =>
        Logger.debug(s"Http response code from Login micro service was: ${resp.status}")
        resp.json.as[LoginResponse]
    }
  }
}

