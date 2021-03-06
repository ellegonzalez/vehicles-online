package controllers

import Common.PrototypeHtml
import helpers.disposal_of_vehicle.CookieFactoryForUnitSpecs
import helpers.{UnitSpec, TestWithApplication}
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, OK}
import uk.gov.dvla.vehicles.presentation.common.clientsidesession.ClientSideSessionFactory
import utils.helpers.Config

class DisposeFailureUnitSpec extends UnitSpec {
  "present" should {
    "display the page" in new TestWithApplication {
      whenReady(present) { r =>
        r.header.status should equal(OK)
      }
    }

    "display prototype message when config set to true" in new TestWithApplication {
      contentAsString(present) should include(PrototypeHtml)
    }

    "not display prototype message when config set to false" in new TestWithApplication {
      val request = FakeRequest()
      implicit val clientSideSessionFactory = injector.getInstance(classOf[ClientSideSessionFactory])
      implicit val config: Config = mock[Config]
      when(config.isPrototypeBannerVisible).thenReturn(false) // Stub this config value.
      val disposeFailurePrototypeNotVisible = new DisposeFailure()

      val result = disposeFailurePrototypeNotVisible.present(request)
      contentAsString(result) should not include PrototypeHtml
    }
  }

  private lazy val present = {
    val disposeFailure = injector.getInstance(classOf[DisposeFailure])
    val request = FakeRequest()
      .withCookies(CookieFactoryForUnitSpecs.traderDetailsModel())
      .withCookies(CookieFactoryForUnitSpecs.vehicleAndKeeperDetailsModel())
      .withCookies(CookieFactoryForUnitSpecs.disposeFormModel())
      .withCookies(CookieFactoryForUnitSpecs.disposeTransactionId())
    disposeFailure.present(request)
  }
}