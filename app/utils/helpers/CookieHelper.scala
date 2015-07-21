package utils.helpers

import controllers.routes
import models.SeenCookieMessageCacheKey
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{DiscardingCookie, RequestHeader, Result}
import uk.gov.dvla.vehicles.presentation.common.clientsidesession.CookieImplicits.RichCookies
import scala.concurrent.Future

object CookieHelper {
  def discardAllCookies(implicit request: RequestHeader): Result = {
    Logger.warn(s"Removing all cookies except seen cookie.")

    val discardingCookiesKeys = request.cookies.map(_.name).filter(_ != SeenCookieMessageCacheKey)
    val discardingCookies = discardingCookiesKeys.map(DiscardingCookie(_)).toSeq
    Redirect(routes.BeforeYouStart.present())
      .discardingCookies(discardingCookies: _*)

  }
}
