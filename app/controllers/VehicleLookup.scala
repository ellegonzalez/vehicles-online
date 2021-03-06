package controllers

import com.google.inject.Inject
import models.DisposeCacheKeyPrefix.CookiePrefix
import models.DisposeFormModel.DisposeOccurredCacheKey
import models.DisposeFormModel.PreventGoingToDisposePageCacheKey
import models.DisposeFormModel.SurveyRequestTriggerDateCacheKey
import models.{EnterAddressManuallyFormModel, VehicleLookupViewModel, AllCacheKeys, VehicleLookupFormModel}
import play.api.data.{Form, FormError}
import play.api.mvc.{Action, Request, Result}
import scala.concurrent.Future
import uk.gov.dvla.vehicles.presentation.common
import common.clientsidesession.ClientSideSessionFactory
import common.clientsidesession.CookieImplicits.{RichCookies, RichForm, RichResult}
import common.controllers.VehicleLookupBase
import common.model.{BruteForcePreventionModel, TraderDetailsModel, VehicleAndKeeperDetailsModel}
import common.model.MicroserviceResponseModel.MsResponseCacheKey
import common.services.DateService
import common.views.helpers.FormExtensions.formBinding
import common.webserviceclients.bruteforceprevention.BruteForcePreventionService
import common.webserviceclients.vehicleandkeeperlookup.VehicleAndKeeperLookupDetailsDto
import common.webserviceclients.vehicleandkeeperlookup.VehicleAndKeeperLookupFailureResponse
import common.webserviceclients.vehicleandkeeperlookup.VehicleAndKeeperLookupService
import utils.helpers.Config

class VehicleLookup @Inject()(implicit bruteForceService: BruteForcePreventionService,
                              vehicleAndKeeperLookupService: VehicleAndKeeperLookupService,
                              surveyUrl: SurveyUrl,
                              dateService: DateService,
                              clientSideSessionFactory: ClientSideSessionFactory,
                              config: Config)
  extends VehicleLookupBase[VehicleLookupFormModel] with BusinessController {

  override val form = Form(VehicleLookupFormModel.Form.Mapping)
  override val responseCodeCacheKey: String = MsResponseCacheKey

  protected val submitTarget = controllers.routes.VehicleLookup.submit()
  protected val backTarget = controllers.routes.VehicleLookup.back()
  protected val exitTarget = controllers.routes.VehicleLookup.exit()
  protected val onVrmLocked = Redirect(routes.VrmLocked.present())
  protected val onMicroServiceError = Redirect(routes.MicroServiceError.present())
  protected val onVehicleLookupFailure = Redirect(routes.VehicleLookupFailure.present())
  protected val missingTradeDetails = Redirect(routes.SetUpTradeDetails.present())
  protected val resetTradeDetails = routes.SetUpTradeDetails.reset()
  protected val enterAddressManually = Redirect(routes.EnterAddressManually.present())
  protected val businessChooseYourAddress = Redirect(routes.BusinessChooseYourAddress.present())
  protected val suppressedV5C = Redirect(routes.SuppressedV5C.present())
  protected val duplicateDisposalError = Redirect(routes.DuplicateDisposalError.present())
  protected val dispose = Redirect(routes.Dispose.present())
  protected val onExit = Redirect(routes.BeforeYouStart.present())

  override def vrmLocked(bruteForcePreventionModel: BruteForcePreventionModel, formModel: VehicleLookupFormModel)
                        (implicit request: Request[_]): Result = onVrmLocked

  override def microServiceError(t: Throwable, formModel: VehicleLookupFormModel)
                                (implicit request: Request[_]): Result = onMicroServiceError

  override def vehicleLookupFailure(failure: VehicleAndKeeperLookupFailureResponse, formModel: VehicleLookupFormModel)
                                   (implicit request: Request[_]): Result = onVehicleLookupFailure

  override def presentResult(implicit request: Request[_]) = {
    request.cookies.getModel[TraderDetailsModel] match {
      case Some(traderDetails) =>
        logMessage(request.cookies.trackingId(), Info, "Presenting vehicle lookup view")
        Ok(views.html.disposal_of_vehicle.vehicle_lookup(
        VehicleLookupViewModel(
          form.fill(),
          shouldDisplayExitButton(request, clientSideSessionFactory),
          surveyUrl(request, isPrivateKeeper = isPrivateKeeper),
          traderDetails.traderName,
          traderDetails.traderAddress.address,
          traderDetails.traderEmail,
          submitTarget,
          backTarget,
          exitTarget,
          resetTradeDetails
        )))
      case None =>
        logMessage(request.cookies.trackingId(), Error,
          s"Failed to find dealer details, redirecting to ${routes.SetUpTradeDetails.present()}")
        missingTradeDetails
    }
  }

  override def invalidFormResult(invalidForm: Form[VehicleLookupFormModel])
                                (implicit request: Request[_]): Future[Result] = Future.successful {
    request.cookies.getModel[TraderDetailsModel] match {
      case Some(traderDetails) =>
        BadRequest(views.html.disposal_of_vehicle.vehicle_lookup(
          VehicleLookupViewModel(
            formWithReplacedErrors(invalidForm),
            shouldDisplayExitButton(request, clientSideSessionFactory),
            surveyUrl(request, isPrivateKeeper = isPrivateKeeper),
            traderDetails.traderName,
            traderDetails.traderAddress.address,
            traderDetails.traderEmail,
            submitTarget,
            backTarget,
            exitTarget,
            resetTradeDetails
          ))
        )
      case None =>
        logMessage(request.cookies.trackingId(), Error,
          s"Failed to find dealer details, redirecting to ${routes.SetUpTradeDetails.present()}")
        missingTradeDetails
    }
  }

  override def vehicleFoundResult(vehicleAndKeeperDetailsDto: VehicleAndKeeperLookupDetailsDto,
                                  formModel: VehicleLookupFormModel)
                                 (implicit request: Request[_]): Result = {

    val model = VehicleAndKeeperDetailsModel.from(vehicleAndKeeperDetailsDto)
    val suppressed = model.suppressedV5Flag.getOrElse(false)
    val disposed = model.keeperEndDate.isDefined

    (disposed, suppressed) match {
      case (_, true) =>
        logMessage(request.cookies.trackingId(), Info,
          s"The vehicle has suppressed V5C flag switched on so redirecting from VehicleLookup to ${routes.SuppressedV5C.present()}}")
        suppressedV5C.withCookie(model)
      case (true, false) =>
        logMessage(request.cookies.trackingId(), Info,
          s"The vehicle has already been disposed so redirecting from VehicleLookup to ${routes.DuplicateDisposalError.present()}}")
        duplicateDisposalError
      case (false, _) =>
        logMessage(request.cookies.trackingId(), Info,
          s"Redirecting from VehicleLookup to $PreventGoingToDisposePageCacheKey}")
        dispose
          .withCookie(VehicleAndKeeperDetailsModel.from(vehicleAndKeeperDetailsDto))
          .discardingCookie(PreventGoingToDisposePageCacheKey)
    }
  }

  def exit = Action { implicit request =>
    logMessage(request.cookies.trackingId(), Info,
      s"Redirecting from VehicleLookup to ${routes.BeforeYouStart.present()}}")
    onExit
      .discardingCookies(AllCacheKeys)
      .withCookie(SurveyRequestTriggerDateCacheKey, dateService.now.getMillis.toString)
  }

  def back = Action { implicit request =>
    request.cookies.getModel[EnterAddressManuallyFormModel] match {
      case Some(manualAddress) =>
        logMessage(request.cookies.trackingId(), Info,
          s"Manual address entry found so redirecting to ${routes.EnterAddressManually.present()}}")
        enterAddressManually
      case None =>
        logMessage(request.cookies.trackingId(), Info,
          s"Manual address entry not found so redirecting to ${routes.BusinessChooseYourAddress.present()}}")
        businessChooseYourAddress
    }
  }

  private def formWithReplacedErrors(invalidForm: Form[VehicleLookupFormModel]) =
    invalidForm.replaceError(
      VehicleLookupFormModel.Form.VehicleRegistrationNumberId,
      FormError(
        key = VehicleLookupFormModel.Form.VehicleRegistrationNumberId,
        message = "error.restricted.validVrnOnly",
        args = Seq.empty
      )
    ).replaceError(
      VehicleLookupFormModel.Form.DocumentReferenceNumberId,
      FormError(
        key = VehicleLookupFormModel.Form.DocumentReferenceNumberId,
        message = "error.validDocumentReferenceNumber",
        args = Seq.empty
      )
    ).distinctErrors

  private def shouldDisplayExitButton(implicit request: Request[_],
                                      clientSideSessionFactory: ClientSideSessionFactory): Boolean = {
    val session = clientSideSessionFactory.getSession(request.cookies)
    val encryptedCookieName = session.nameCookie(DisposeOccurredCacheKey).value
    val displayExitButton = request.cookies.exists(c => c.name == encryptedCookieName)
    displayExitButton
  }
}
