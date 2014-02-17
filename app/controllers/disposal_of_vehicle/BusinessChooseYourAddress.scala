package controllers.disposal_of_vehicle

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import models.domain.disposal_of_vehicle.{DealerDetailsModel, BusinessChooseYourAddressModel}
import mappings.disposal_of_vehicle.BusinessAddressSelect._
import mappings.common.DropDown
import DropDown._
import controllers.disposal_of_vehicle.Helpers._
import play.api.Logger
import play.api.Play.current
import mappings.disposal_of_vehicle.DealerDetails
import javax.inject.Inject

class BusinessChooseYourAddress @Inject() (addressLookupService: services.AddressLookupService) extends Controller {
  val fetchAddresses = addressLookupService.fetchAddress("TEST") // TODO pass in postcode submitted on the previous page.

  val form = Form(
    mapping(
      addressSelectId -> dropDown(fetchAddresses)
    )(BusinessChooseYourAddressModel.apply)(BusinessChooseYourAddressModel.unapply)
  )

  def present = Action { implicit request =>
    fetchDealerNameFromCache match {
      case Some(name) => Ok(views.html.disposal_of_vehicle.business_choose_your_address(form, name, fetchAddresses))
      case None => Redirect(routes.SetUpTradeDetails.present)
    }
  }

  def submit = Action { implicit request =>
    form.bindFromRequest.fold(
      formWithErrors =>
        fetchDealerNameFromCache match {
          case Some(name) => BadRequest(views.html.disposal_of_vehicle.business_choose_your_address(formWithErrors, name, fetchAddresses))
          case None => {
            Logger.error("failed to find dealer name in cache for formWithErrors, redirecting...")
            Redirect(routes.SetUpTradeDetails.present)
          }
        },
      f =>
        fetchDealerNameFromCache match {
          case Some(name) => {
            storeDealerDetailsInCache(f, name)
            Redirect(routes.VehicleLookup.present)
          }
          case None => {
            Logger.error("failed to find dealer name in cache on submit, redirecting...")
            Redirect(routes.SetUpTradeDetails.present)
          }
        }
    )
  }

  private def storeDealerDetailsInCache(model: BusinessChooseYourAddressModel, dealerName: String) = {
    val key = DealerDetails.cacheKey
    val value = DealerDetailsModel(dealerName = dealerName, dealerAddress = addressLookupService.lookupAddress(model.addressSelected))
    play.api.cache.Cache.set(key, value)
    Logger.debug(s"BusinessChooseYourAddress stored data in cache: key = $key, value = ${value}")
  }
}