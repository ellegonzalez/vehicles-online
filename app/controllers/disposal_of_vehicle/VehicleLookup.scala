package controllers.disposal_of_vehicle

import play.api.mvc._
import play.api.data.{FormError, Form}
import play.api.data.Forms._
import play.api.Logger
import mappings.common.{ReferenceNumber, RegistrationNumber, Consent}
import ReferenceNumber._
import RegistrationNumber._
import mappings.disposal_of_vehicle.VehicleLookup._
import Consent._
import models.domain.disposal_of_vehicle.{VehicleDetailsRequest, VehicleDetailsModel, VehicleLookupFormModel}
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import com.google.inject.Inject
import controllers.disposal_of_vehicle.Helpers._
import controllers.disposal_of_vehicle.Helpers.{storeVehicleDetailsInCache, storeVehicleLookupFormModelInCache}
import services.vehicle_lookup.VehicleLookupService
import utils.helpers.FormExtensions._

class VehicleLookup @Inject()(webService: VehicleLookupService) extends Controller {

  val vehicleLookupForm = Form(
    mapping(
      referenceNumberId -> referenceNumber,
      registrationNumberId -> registrationNumber
    )(VehicleLookupFormModel.apply)(VehicleLookupFormModel.unapply)
  )

  def present = Action { implicit request =>
    fetchDealerDetailsFromCache match {
      case Some(dealerDetails) => Ok(views.html.disposal_of_vehicle.vehicle_lookup(dealerDetails, vehicleLookupForm))
      case None => Redirect(routes.SetUpTradeDetails.present)
    }
  }

  def submit = Action.async { implicit request =>
    vehicleLookupForm.bindFromRequest.fold(
      formWithErrors =>
        Future {
          fetchDealerDetailsFromCache match {
            case Some(dealerDetails) =>
              val formWithReplacedErrors = formWithErrors.
                replaceError(registrationNumberId, "error.minLength", FormError(key = registrationNumberId, message = "error.restricted.validVRNOnly", args = Seq.empty)).
                replaceError(registrationNumberId, "error.maxLength", FormError(key = registrationNumberId, message = "error.restricted.validVRNOnly", args = Seq.empty)).
                replaceError(registrationNumberId, "error.required", FormError(key = registrationNumberId, message = "error.restricted.validVRNOnly", args = Seq.empty)).

                replaceError(referenceNumberId, "error.minLength", FormError(key = referenceNumberId, message = "error.validDocumentReferenceNumber", args = Seq.empty)).
                replaceError(referenceNumberId, "error.maxLength", FormError(key = referenceNumberId, message = "error.validDocumentReferenceNumber", args = Seq.empty)).
                replaceError(referenceNumberId, "error.required", FormError(key = referenceNumberId, message = "error.validDocumentReferenceNumber", args = Seq.empty)).
                replaceError(referenceNumberId, "error.restricted.validNumberOnly", FormError(key = referenceNumberId, message = "error.validDocumentReferenceNumber", args = Seq.empty)).
                distinctErrors
              BadRequest(views.html.disposal_of_vehicle.vehicle_lookup(dealerDetails, formWithReplacedErrors))
            case None => Redirect(routes.SetUpTradeDetails.present)
          }
        },
      f => {
        val modelWithoutSpaces = f.copy(registrationNumber = f.registrationNumber.replace(" ","")) // DE7 Strip spaces from input as it is not allowed in the micro-service.
        lookupVehicle(webService, modelWithoutSpaces)
      }
    )
  }

  def back = Action { implicit request =>
    fetchDealerDetailsFromCache match {
      case Some(dealerDetails) =>
        if (dealerDetails.dealerAddress.uprn.isDefined) Redirect(routes.BusinessChooseYourAddress.present)
        else Redirect(routes.EnterAddressManually.present)
      case None => Redirect(routes.SetUpTradeDetails.present)
    }
  }

  private def lookupVehicle(webService: VehicleLookupService, model: VehicleLookupFormModel): Future[SimpleResult] = {
    webService.invoke(buildMicroServiceRequest(model)).map { resp =>
      Logger.debug(s"VehicleLookup Web service call successful - response = ${resp}")
      // TODO Don't save these two models, instead we need a combined model that has what the user entered into the form plus the micro-service response.
      storeVehicleLookupFormModelInCache(model)
      if (resp.success) {
        storeVehicleDetailsInCache(VehicleDetailsModel.fromDto(resp.vehicleDetailsDto))
        Redirect(routes.Dispose.present)
      }
      else Redirect(routes.VehicleLookupFailure.present)
    }.recover {
      case e: Throwable => {
        Logger.debug(s"Web service call failed. Exception: ${e}")
        BadRequest("The remote server didn't like the request.") // TODO check the user story for going to an error message page when micro-service kaput (not the same as a system failure).
      }
    }
  }

  private def buildMicroServiceRequest(formModel: VehicleLookupFormModel):VehicleDetailsRequest = {
    VehicleDetailsRequest(referenceNumber = formModel.referenceNumber, registrationNumber = formModel.registrationNumber)
  }

}


