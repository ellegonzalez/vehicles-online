package controllers.disposal_of_vehicle

import common.CookieHelper._
import helpers.{UnitSpec, WithApplication}
import pages.disposal_of_vehicle._
import play.api.Play
import play.api.test.FakeRequest
import play.api.test.Helpers._
import mappings.common.Languages._
import controllers.Language.withLanguage

final class LanguagesUnitSpec extends UnitSpec {
  "withLanguageCy" should {
    "redirect back to the same page" in new WithApplication {
      val result = withLanguage(CyId)(request)
      whenReady(result) {
        r =>
          r.header.status should equal(SEE_OTHER) // Redirect...
          r.header.headers.get(LOCATION) should equal(Some(BeforeYouStartPage.address)) // ... back to the same page.
      }
    }

    "writes language cookie set to 'cy'" in new WithApplication {
      val result = withLanguage(CyId)(request)
      whenReady(result) {
        r =>
          val cookies = fetchCookiesFromHeaders(r)
          cookies.find(_.name == Play.langCookieName) match {
            case Some(cookie) => cookie.value should equal("cy")
            case None => fail("langCookieName not found")
          }
      }
    }
  }

  "withLanguageEn" should {
    "redirect back to the same page" in new WithApplication {
      val result = withLanguage(EnId)(request)
      whenReady(result) {
        r =>
          r.header.status should equal(SEE_OTHER) // Redirect...
          r.header.headers.get(LOCATION) should equal(Some(BeforeYouStartPage.address)) // ... back to the same page.
      }
    }

    "writes language cookie set to 'en'" in new WithApplication {
      val result = withLanguage(EnId)(request)
      whenReady(result) {
        r =>
          val cookies = fetchCookiesFromHeaders(r)
          cookies.find(_.name == Play.langCookieName) match {
            case Some(cookie) => cookie.value should equal("en")
            case None => fail("langCookieName not found")
          }
      }
    }
  }

  private val request = FakeRequest().withHeaders("Referer" -> BeforeYouStartPage.address)
}
