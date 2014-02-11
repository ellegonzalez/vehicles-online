package controllers.disposal_of_vehicle

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import controllers.Mappings._
import models.domain.disposal_of_vehicle.VehicleLookupModel
import app.DisposalOfVehicle.VehicleLookup._

object VehicleLookup extends Controller {

  val vehicleLookupForm = Form(
    mapping(
      v5cReferenceNumberID -> V5cReferenceNumber(minLength = 11, maxLength = 11),
      v5cRegistrationNumberID -> V5CRegistrationNumber(minLength = 2, maxLength = 8),
      v5cKeeperNameID -> nonEmptyText(minLength = 2, maxLength = 100),
      v5cPostcodeID -> Postcode(minLength = 5, maxLength = 8)
    )(VehicleLookupModel.apply)(VehicleLookupModel.unapply)
  )

  def present = Action {
    implicit request =>
      Ok(views.html.disposal_of_vehicle.vehicle_lookup(vehicleLookupForm))
  }

  def submit = Action {
    implicit request => {
      vehicleLookupForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.disposal_of_vehicle.vehicle_lookup(formWithErrors)),
        f => Redirect(routes.Dispose.present)
      )
    }
  }
}