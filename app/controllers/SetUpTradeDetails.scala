package controllers

import com.google.inject.Inject
import models.AllCacheKeys
import models.DisposeCacheKeyPrefix.CookiePrefix
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}
import play.api.mvc.{Result, Request}
import uk.gov.dvla.vehicles.presentation.common.clientsidesession.ClientSideSessionFactory
import uk.gov.dvla.vehicles.presentation.common.clientsidesession.CookieImplicits.RichResult
import uk.gov.dvla.vehicles.presentation.common.controllers.SetUpTradeDetailsBase
import uk.gov.dvla.vehicles.presentation.common.mappings.BusinessName.businessNameMapping
import uk.gov.dvla.vehicles.presentation.common.mappings.Email.email
import uk.gov.dvla.vehicles.presentation.common.mappings.Postcode.postcode
import uk.gov.dvla.vehicles.presentation.common.model.SetupTradeDetailsFormModel
import uk.gov.dvla.vehicles.presentation.common.model.SetupTradeDetailsFormModel.Form.TraderEmailId
import uk.gov.dvla.vehicles.presentation.common.model.SetupTradeDetailsFormModel.Form.TraderNameId
import uk.gov.dvla.vehicles.presentation.common.model.SetupTradeDetailsFormModel.Form.TraderPostcodeId
import utils.helpers.Config

class SetUpTradeDetails @Inject()()(implicit clientSideSessionFactory: ClientSideSessionFactory,
                                    config: Config) extends SetUpTradeDetailsBase {

  protected val submitTarget = controllers.routes.SetUpTradeDetails.submit()
  protected val onSuccess = Redirect(routes.BusinessChooseYourAddress.present())

  override val form = Form(
    mapping(
      TraderNameId -> businessNameMapping,
      TraderPostcodeId -> postcode,
      TraderEmailId -> optional(email)
    )(SetupTradeDetailsFormModel.apply)(SetupTradeDetailsFormModel.unapply)
  )

  override def presentResult(model: Form[SetupTradeDetailsFormModel])(implicit request: Request[_]): Result =
    Ok(views.html.disposal_of_vehicle.setup_trade_details(model, submitTarget)).
      withNewSession.
      discardingCookies(AllCacheKeys)

  override def invalidFormResult(model: Form[SetupTradeDetailsFormModel])(implicit request: Request[_]): Result =
    BadRequest(views.html.disposal_of_vehicle.setup_trade_details(model, submitTarget))

  override def success(implicit request: Request[_]): Result =
    onSuccess
}
