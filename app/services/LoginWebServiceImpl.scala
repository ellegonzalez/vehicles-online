package services

import models.domain.change_of_address._
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import utils.helpers.Environment
import models.domain.common.Address

class LoginWebServiceImpl() extends LoginWebService {
  implicit val writeLoginPage = Json.writes[LoginPageModel]
  implicit val address = Json.reads[Address]
  implicit val loginConfirmationModel = Json.reads[LoginConfirmationModel]
  implicit val loginResponse = Json.reads[LoginResponse]

  override def invoke(cmd: LoginPageModel): Future[LoginResponse] = {
    val endPoint = s"${Environment.microServiceUrlBase}/login-page"
    Logger.debug(s"Calling Login micro service on ${endPoint}...")
    val futureOfResponse = WS.url(endPoint).post(Json.toJson(cmd))

    futureOfResponse.map { resp =>
      Logger.debug(s"Http response code from Login micro service was: ${resp.status}")
      resp.json.as[LoginResponse]
    }
  }
}

