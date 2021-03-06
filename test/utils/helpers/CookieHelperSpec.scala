package utils.helpers

import composition.TestGlobalWithFilters
import helpers.disposal_of_vehicle.CookieFactoryForUnitSpecs
import helpers.TestWithApplication
import helpers.UnitSpec
import models.AllCacheKeys
import pages.disposal_of_vehicle.BeforeYouStartPage
import play.api.test.Helpers.LOCATION
import play.api.test.FakeRequest
import uk.gov.dvla.vehicles.presentation.common.utils.helpers.CookieHelper
import uk.gov.dvla.vehicles.presentation.common.testhelpers.LightFakeApplication
import uk.gov.dvla.vehicles.presentation.common.testhelpers.CookieHelper.fetchCookiesFromHeaders

final class CookieHelperSpec extends UnitSpec {
  "handleApplicationSecretChange" should {
    "discard all cookies except SeenCookieMessageKey" in new TestWithApplication(appWithCryptpConfig) {
      val request = FakeRequest().
        withCookies(CookieFactoryForUnitSpecs.seenCookieMessage()).
        withCookies(CookieFactoryForUnitSpecs.setupTradeDetails()).
        withCookies(CookieFactoryForUnitSpecs.traderDetailsModel()).
        withCookies(CookieFactoryForUnitSpecs.vehicleAndKeeperDetailsModel()).
        withCookies(CookieFactoryForUnitSpecs.disposeFormModel()).
        withCookies(CookieFactoryForUnitSpecs.disposeTransactionId()).
        withCookies(CookieFactoryForUnitSpecs.vehicleRegistrationNumber())

      val result = CookieHelper.discardAllCookies(controllers.routes.BeforeYouStart.present)(request)

      val cookies = fetchCookiesFromHeaders(result)
      cookies.filter(cookie => AllCacheKeys.contains(cookie.name)).foreach { cookie =>
        cookie.maxAge match {
          case Some(maxAge) if maxAge < 0 => // Success
          case Some(maxAge) => fail(s"maxAge should be negative but was $maxAge")
          case _ => fail("should be some maxAge")
        }
      }
    }

    "redirect to BeforeYouStart page" in new TestWithApplication(appWithCryptpConfig) {
      val request = FakeRequest().
        withCookies(CookieFactoryForUnitSpecs.seenCookieMessage()).
        withCookies(CookieFactoryForUnitSpecs.setupTradeDetails()).
        withCookies(CookieFactoryForUnitSpecs.traderDetailsModel()).
        withCookies(CookieFactoryForUnitSpecs.vehicleAndKeeperDetailsModel()).
        withCookies(CookieFactoryForUnitSpecs.disposeFormModel()).
        withCookies(CookieFactoryForUnitSpecs.disposeTransactionId()).
        withCookies(CookieFactoryForUnitSpecs.vehicleRegistrationNumber())

      val result = CookieHelper. discardAllCookies(controllers.routes.BeforeYouStart.present)(request)

      result.header.headers.get(LOCATION) should equal(Some(BeforeYouStartPage.address))

    }
  }

  private val appWithCryptpConfig =
    LightFakeApplication(TestGlobalWithFilters,Map("application.secret256Bit" -> "MnPSvGpiEF5OJRG3xLAnsfmdMTLr6wpmJmZLv2RB9Vo="))

}
