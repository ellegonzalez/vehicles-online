package views.disposal_of_vehicle

import composition.TestHarness
import helpers.UiSpec
import helpers.disposal_of_vehicle.CookieFactoryForUISpecs
import helpers.tags.UiTag
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.WebBrowser
import WebBrowser.click
import WebBrowser.go
import WebBrowser.pageSource
import WebBrowser.pageTitle
import pages.disposal_of_vehicle.VehicleLookupFailurePage.{beforeYouStart, vehicleLookup}
import pages.disposal_of_vehicle.BeforeYouStartPage
import pages.disposal_of_vehicle.SetupTradeDetailsPage
import pages.disposal_of_vehicle.VehicleLookupPage
import pages.disposal_of_vehicle.VehicleLookupFailurePage
import webserviceclients.fakes.brute_force_protection.FakeBruteForcePreventionWebServiceImpl.MaxAttempts

class VehicleLookupFailureIntegrationSpec extends UiSpec with TestHarness {
  private final val VrmNotFound = "vehicle_and_keeper_lookup_vrm_not_found"
  private final val DocumentReferenceMismatch = "vehicle_and_keeper_lookup_document_reference_mismatch"
  val expectedString = "You will only have a limited number of attempts to enter the vehicle details for this vehicle."

  "go to page" should {

    "display the page" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()
      go to VehicleLookupFailurePage
      pageTitle should equal(VehicleLookupFailurePage.title)
    }

    "redirect to setuptrade details if cache is empty on page load" taggedAs UiTag in new WebBrowserForSelenium {
      go to VehicleLookupFailurePage
      pageTitle should equal(SetupTradeDetailsPage.title)
    }

    "redirect to setuptrade details if only VehicleLookupFormModelCache is populated" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      CookieFactoryForUISpecs.vehicleLookupFormModel()
      go to VehicleLookupFailurePage
      pageTitle should equal(SetupTradeDetailsPage.title)
    }

    "redirect to setuptrade details if only dealerDetails cache is populated" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      CookieFactoryForUISpecs.dealerDetails()
      go to VehicleLookupFailurePage
      pageTitle should equal(SetupTradeDetailsPage.title)
    }

    "display messages that show that the number of brute force attempts does not impact which messages " +
      "are displayed when 1 attempt has been made" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage

      CookieFactoryForUISpecs.
        dealerDetails().
        bruteForcePreventionViewModel(attempts = 1, maxAttempts = MaxAttempts).
        vehicleLookupFormModel().
        vehicleLookupResponse(responseMessage = VrmNotFound)

      go to VehicleLookupFailurePage
      pageSource should include(expectedString)
    }

    "display messages that show that the number of brute force attempts does not impact which messages are " +
      "displayed when 2 attempts have been made" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage

      CookieFactoryForUISpecs.
        dealerDetails().
        bruteForcePreventionViewModel(attempts = 2, maxAttempts = MaxAttempts).
        vehicleLookupFormModel().
        vehicleLookupResponse(responseMessage = VrmNotFound)

      go to VehicleLookupFailurePage
      pageSource should include(expectedString)
    }

    "display appropriate messages for document reference mismatch" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage

      CookieFactoryForUISpecs.
        dealerDetails().
        bruteForcePreventionViewModel().
        vehicleLookupFormModel().
        vehicleLookupResponse(responseMessage = DocumentReferenceMismatch)

      go to VehicleLookupFailurePage
      pageSource should include(expectedString)
    }

  }

  "vehicleLookup button" should {
    "redirect to vehiclelookup when button clicked" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()
      go to VehicleLookupFailurePage
      click on vehicleLookup
      pageTitle should equal(VehicleLookupPage.title)
    }
  }

  "beforeYouStart button" should {
    "redirect to beforeyoustart" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()
      go to VehicleLookupFailurePage
      click on beforeYouStart
      pageTitle should equal(BeforeYouStartPage.title)
    }
  }

  private def cacheSetup()(implicit webDriver: WebDriver) =
    CookieFactoryForUISpecs.
      dealerDetails().
      bruteForcePreventionViewModel().
      vehicleLookupFormModel().
      vehicleLookupResponse()
}
