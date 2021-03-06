package controllers

import com.google.inject.Inject
import models.DisposeCacheKeyPrefix.CookiePrefix
import models.DisposeFormModel.PreventGoingToDisposePageCacheKey
import java.util.Locale
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import play.api.mvc.Action
import uk.gov.dvla.vehicles.presentation.common
import common.clientsidesession.ClientSideSessionFactory
import common.clientsidesession.CookieImplicits.{RichCookies, RichResult}
import utils.helpers.Config

class MicroServiceError @Inject()(implicit clientSideSessionFactory: ClientSideSessionFactory,
                                  config: Config) extends BusinessController {
  
  protected val defaultRedirectUrl = controllers.routes.VehicleLookup.present().url
  protected val tryAgainTarget = controllers.routes.MicroServiceError.back()
  protected val exitTarget = controllers.routes.BeforeYouStart.present()

  def present = Action { implicit request =>
    val trackingId = request.cookies.trackingId()
    logMessage(trackingId, Debug, "Displaying MicroServiceError page")

    val referer = request.headers.get(REFERER).getOrElse(defaultRedirectUrl)
    logMessage(request.cookies.trackingId(), Debug, s"Referer $referer")
    logMessage(request.cookies.trackingId(), Debug, s"Try again target $tryAgainTarget")

    ServiceUnavailable(
      views.html.disposal_of_vehicle.micro_service_error(
        h(config.openingTimeMinOfDay * MillisInMinute),
        h(config.closingTimeMinOfDay * MillisInMinute),
        tryAgainTarget,
        exitTarget
      )
    )
    // Save the previous page URL (from the referrer header) into a cookie.
    .withCookie(MicroServiceError.MicroServiceErrorRefererCacheKey, referer)
    // Remove the interstitial cookie so we do not get bounced back to vehicle lookup unless we were on that page
    .discardingCookie(PreventGoingToDisposePageCacheKey)
  }

  def back = Action { implicit request =>
    val referer: String = request.cookies
      .getString(MicroServiceError.MicroServiceErrorRefererCacheKey)
      .getOrElse(defaultRedirectUrl)
    Redirect(referer).discardingCookie(MicroServiceError.MicroServiceErrorRefererCacheKey)
  }

  private final val MillisInMinute = 60 * 1000L

  private def h(hourMillis: Long) =
    DateTimeFormat.forPattern("HH:mm").withLocale(Locale.UK)
      .print(new DateTime(hourMillis, DateTimeZone.forID("UTC"))).toLowerCase // Must use UTC as we only want to format the hour
}

object MicroServiceError {
  final val MicroServiceErrorRefererCacheKey = s"${CookiePrefix}msError"
}
