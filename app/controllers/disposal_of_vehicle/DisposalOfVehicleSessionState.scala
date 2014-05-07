package controllers.disposal_of_vehicle

import play.api.Logger
import models.domain.disposal_of_vehicle._
import models.domain.disposal_of_vehicle.{DealerDetailsModel, DisposeFormModel, VehicleLookupFormModel, SetupTradeDetailsModel, VehicleDetailsModel}
import scala.Some
import mappings.disposal_of_vehicle.SetupTradeDetails._
import mappings.disposal_of_vehicle.BusinessChooseYourAddress._
import mappings.disposal_of_vehicle.DealerDetails._
import mappings.disposal_of_vehicle.Dispose._
import mappings.disposal_of_vehicle.VehicleLookup._
import play.api.mvc.{Request, Cookie, SimpleResult}
import play.api.libs.json.{JsPath, Json}
import play.api.data.validation.ValidationError
import models.domain.disposal_of_vehicle.SetupTradeDetailsModel.setupTradeDetailsModelFormat
import play.api.libs.Crypto
import utils.helpers.CryptoHelper

case class JsonValidationException(errors: Seq[(JsPath, Seq[ValidationError])]) extends Exception

object DisposalOfVehicleSessionState2 { // TODO: This is the new way of doing caching. Remove old version piece by piece into the new style then rename this.

  implicit class RequestAdapter[A](val request: Request[A]) extends AnyVal {
    def fetchDealerDetailsFromCache: Option[SetupTradeDetailsModel] = request.cookies.get(CryptoHelper.encryptCookieName(SetupTradeDetailsCacheKey)) match {
      case Some(cookie) =>
        val decrypted = CryptoHelper.decryptCookie(cookie.value)
        val parsed = Json.parse(decrypted)
        val fromJson = Json.fromJson[SetupTradeDetailsModel](parsed)
        fromJson.asEither match {
          case Left(errors) => throw JsonValidationException(errors)
          case Right(model) => Some(model)
        }
      case None => None
    }

    def fetchDealerNameFromCache: Option[String] = {
      fetchDealerDetailsFromCache match {
        case Some(model) => Some(model.traderBusinessName)
        case None => None
      }
    }
  }

  implicit class SimpleResultAdapter(val result: SimpleResult) extends AnyVal {
    def withTradeDetailsInCache(model: SetupTradeDetailsModel): SimpleResult = {
      val stateAsJson = Json.toJson(model)
      val encryptedStateAsJson = CryptoHelper.encryptCookie(stateAsJson.toString)
      val cookie = Cookie(CryptoHelper.encryptCookieName(SetupTradeDetailsCacheKey), encryptedStateAsJson)
      result.withCookies(cookie)
    }
  }
}

import services.session.SessionState
import com.google.inject.Inject

class DisposalOfVehicleSessionState @Inject()(val inner: SessionState) {

  def storeDealerDetailsInCache(model: EnterAddressManuallyModel, dealerName: String) = {
    val dealerAddress = AddressViewModel.from(model.addressAndPostcodeModel)
    val value = DealerDetailsModel(dealerName = dealerName, dealerAddress = dealerAddress)
    inner.set(dealerDetailsCacheKey, Some(value))
    Logger.debug(s"EnterAddressManually stored data in cache: key = $dealerDetailsCacheKey, value = ${value}")
  }

  def storeTradeDetailsInCache(f: SetupTradeDetailsModel) = {
    inner.set(SetupTradeDetailsCacheKey, Some(f))
    Logger.debug(s"SetUpTradeDetails stored data in cache: key = $SetupTradeDetailsCacheKey, value = ${f}")
  }

  def storeBusinessChooseYourAddressModelInCache(value: BusinessChooseYourAddressModel) = {
    inner.set(businessChooseYourAddressCacheKey, Some(value))
    Logger.debug(s"BusinessChooseYourAddress stored BusinessChooseYourAddressModel in cache: key = $businessChooseYourAddressCacheKey, value = ${value}")
  }

  def storeDealerDetailsModelInCache(value: DealerDetailsModel) = {
    inner.set(dealerDetailsCacheKey, Some(value))
    Logger.debug(s"BusinessChooseYourAddress stored DealerDetailsModel in cache: key = $dealerDetailsCacheKey, value = ${value}")
  }

  def storeVehicleDetailsInCache(model: VehicleDetailsModel) = {
    inner.set(vehicleLookupDetailsCacheKey, Some(model))
    Logger.debug(s"VehicleLookup page - stored vehicle details object in cache: key = $vehicleLookupDetailsCacheKey, value = ${model}")
  }

  def storeVehicleLookupResponseCodeInCache(responseCode: String) = {
    inner.set(vehicleLookupResponseCodeCacheKey, Some(responseCode))
    Logger.debug(s"VehicleLookup page - stored vehicle lookup response code in cache: key = $vehicleLookupResponseCodeCacheKey, value = ${responseCode}")
  }

  def storeVehicleLookupFormModelInCache(model: VehicleLookupFormModel) = {
    inner.set(vehicleLookupFormModelCacheKey, Some(model))
    Logger.debug(s"VehicleLookup page - stored vehicle lookup form model details object in cache: key = $vehicleLookupFormModelCacheKey, value = ${model}")
  }

  def storeDisposeFormModelInCache(value: DisposeFormModel) = {
    inner.set(disposeFormModelCacheKey, Some(value))
    Logger.debug(s"Dispose - stored disposeFromModel in cache: key = $disposeFormModelCacheKey, value = $value")
  }

  def storeDisposeTransactionIdInCache(value: String) = {
    inner.set(disposeFormTransactionIdCacheKey, Some(value))
    Logger.debug(s"Dispose - stored dispose transaction id in cache: key = $disposeFormTransactionIdCacheKey, value = $value")
  }

  def storeDisposeTransactionTimestampInCache(value: String) = {
    inner.set(disposeFormTimestampIdCacheKey, Some(value))
    Logger.debug(s"Dispose - stored dispose transaction timestamp in cache: key = $disposeFormTimestampIdCacheKey, value = $value")
  }

  def storeDisposeRegistrationNumberInCache(value: String) = {
    inner.set(disposeFormRegistrationNumberCacheKey, Some(value))
    Logger.debug(s"Dispose - stored dispose registration number in cache: key = $disposeFormRegistrationNumberCacheKey, value = $value")
  }

  def storeDisposeModelInCache(value: DisposeModel) = {
    inner.set(disposeModelCacheKey, Some(value))
    Logger.debug(s"Dispose - stored formModel in cache: key = $disposeModelCacheKey, value = $value")
  }



  def fetchDealerDetailsFromCache: Option[DealerDetailsModel] = inner.get[DealerDetailsModel](dealerDetailsCacheKey)

  def fetchDisposeFormModelFromCache: Option[DisposeFormModel] = inner.get[DisposeFormModel](disposeFormModelCacheKey)

  def fetchVehicleDetailsFromCache: Option[VehicleDetailsModel] = inner.get[VehicleDetailsModel](vehicleLookupDetailsCacheKey)

  def fetchTraderDetailsFromCache: Option[SetupTradeDetailsModel] = inner.get[SetupTradeDetailsModel](SetupTradeDetailsCacheKey)

  def fetchVehicleLookupDetailsFromCache: Option[VehicleLookupFormModel] = inner.get[VehicleLookupFormModel](vehicleLookupFormModelCacheKey)

  def fetchVehicleLookupResponseCodeFromCache: Option[String] = inner.get[String](vehicleLookupResponseCodeCacheKey)

  def fetchBusinessChooseYourAddressModelFromCache: Option[BusinessChooseYourAddressModel] = inner.get[BusinessChooseYourAddressModel](businessChooseYourAddressCacheKey)

  def fetchDisposeTransactionIdFromCache: Option[String] = inner.get[String](disposeFormTransactionIdCacheKey)

  def fetchDisposeTransactionTimestampInCache: Option[String] = inner.get[String](disposeFormTimestampIdCacheKey)

  def fetchDisposeRegistrationNumberFromCache: Option[String] = inner.get[String](disposeFormRegistrationNumberCacheKey)

  def clearVehicleLookupResponseCodeFromCache() = inner.set(vehicleLookupResponseCodeCacheKey, None)
}
