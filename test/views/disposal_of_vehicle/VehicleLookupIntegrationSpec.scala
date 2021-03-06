package views.disposal_of_vehicle

import composition.{TestGlobalWithFilters, TestHarness}
import helpers.disposal_of_vehicle.CookieFactoryForUISpecs
import helpers.tags.UiTag
import helpers.UiSpec
import models.AllCacheKeys
import org.openqa.selenium.{By, WebElement, WebDriver}
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.scalatest.selenium.WebBrowser
import WebBrowser.currentUrl
import WebBrowser.click
import WebBrowser.go
import WebBrowser.pageTitle
import pages.common.ErrorPanel
import pages.disposal_of_vehicle.BeforeYouStartPage
import pages.disposal_of_vehicle.BusinessChooseYourAddressPage
import pages.disposal_of_vehicle.DisposePage
import pages.disposal_of_vehicle.EnterAddressManuallyPage
import pages.disposal_of_vehicle.SetupTradeDetailsPage
import pages.disposal_of_vehicle.VehicleLookupPage
import pages.disposal_of_vehicle.VehicleLookupPage.{happyPath, tryLockedVrm, back, exit}
import pages.disposal_of_vehicle.VrmLockedPage
import uk.gov.dvla.vehicles.presentation.common.filters.CsrfPreventionAction
import uk.gov.dvla.vehicles.presentation.common.testhelpers.LightFakeApplication
import uk.gov.dvla.vehicles.presentation.common.views.widgetdriver.Wait
import webserviceclients.fakes.FakeAddressLookupService.addressWithoutUprn

final class VehicleLookupIntegrationSpec extends UiSpec with TestHarness {

  "go to page" should {
    "display the page" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()

      go to VehicleLookupPage

      pageTitle should equal(VehicleLookupPage.title)
    }


    "Redirect when no traderBusinessName is cached" taggedAs UiTag in new WebBrowserForSelenium {
      go to VehicleLookupPage

      pageTitle should equal(SetupTradeDetailsPage.title)
    }

    "redirect when no dealerBusinessName is cached" taggedAs UiTag in new WebBrowserForSelenium {
      go to VehicleLookupPage

      pageTitle should equal(SetupTradeDetailsPage.title)
    }

    "contain the hidden csrfToken field" taggedAs UiTag in new WebBrowserForSelenium {
      go to VehicleLookupPage
      val csrf: WebElement = webDriver.findElement(By.name(CsrfPreventionAction.TokenName))
      csrf.getAttribute("type") should equal("hidden")
      csrf.getAttribute("name") should
        equal(uk.gov.dvla.vehicles.presentation.common.filters.CsrfPreventionAction.TokenName)
      csrf.getAttribute("value").nonEmpty should equal(true)
    }

    "display the v5c image on the page with Javascript disabled" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()

      go to VehicleLookupPage
      pageTitle should equal(VehicleLookupPage.title)

      Wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//div[@data-tooltip='tooltip_documentReferenceNumber']")),
        5
      )
    }

    "put the v5c image in a tooltip with Javascript enabled" taggedAs UiTag in new WebBrowserWithJs {
      go to BeforeYouStartPage
      cacheSetup()

      go to VehicleLookupPage
      val v5c = By.xpath("//div[@data-tooltip='tooltip_documentReferenceNumber']")
      Wait.until(ExpectedConditions.presenceOfElementLocated(v5c), 5)
      Wait.until(ExpectedConditions.invisibilityOfElementLocated(v5c), 5)
    }

    "have a v5c document reference number input field of type tel" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()

      go to VehicleLookupPage
      val docRef: WebElement = webDriver.findElement(
        By.name(models.VehicleLookupFormModel.Form.DocumentReferenceNumberId)
      )
      docRef.getAttribute("type") should equal("tel")
    }
  }

  "findVehicleDetails button" should {
    "go to the next page when correct data is entered" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()

      happyPath()

      pageTitle should equal(DisposePage.title)
    }

    "display one validation error message when no referenceNumber is entered" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()

      happyPath(referenceNumber = "")

      ErrorPanel.numberOfErrors should equal(1)
    }

    "display one validation error message when no registrationNumber is entered" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()

      happyPath(registrationNumber = "")

      ErrorPanel.numberOfErrors should equal(1)
    }

    "display one validation error message when a registrationNumber " +
      "is entered containing one character" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()

      happyPath(registrationNumber = "a")

      ErrorPanel.numberOfErrors should equal(1)
    }

    "display one validation error message when a registrationNumber is " +
      "entered containing special characters" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()

      happyPath(registrationNumber = "$^")

      ErrorPanel.numberOfErrors should equal(1)
    }

    "display two validation error messages when no vehicle details are " +
      "entered but consent is given" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()

      happyPath(referenceNumber = "", registrationNumber = "")

      ErrorPanel.numberOfErrors should equal(2)
    }

    "display one validation error message when only a valid registrationNumber is " +
      "entered and consent is given" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()

      happyPath(registrationNumber = "")

      ErrorPanel.numberOfErrors should equal(1)
    }

    "does not proceed when milage has non-numeric when invalid referenceNumber (Html5Validation enabled)" taggedAs UiTag in new WebBrowserForSelenium(app = fakeAppWithHtml5ValidationEnabledConfig) {
      go to BeforeYouStartPage
      cacheSetup()

      happyPath(referenceNumber = "INVALID")

      currentUrl should equal(VehicleLookupPage.url)
      ErrorPanel.numberOfErrors should equal(1)
    }

    "display one validation error message when invalid referenceNumber " +
      "(Html5Validation disabled)" taggedAs UiTag in new WebBrowserForSelenium(app = fakeAppWithHtml5ValidationDisabledConfig) {
      go to BeforeYouStartPage
      cacheSetup()

      happyPath(referenceNumber = "")

      ErrorPanel.numberOfErrors should equal(1)
    }

    "redirect to vrm locked when too many attempting to lookup a locked vrm" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()

      tryLockedVrm()
      pageTitle should equal(VrmLockedPage.title)
    }
  }

  "back" should {
    "display BusinessChooseYourAddress page when back link is " +
      "clicked with uprn present" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      CookieFactoryForUISpecs.
        setupTradeDetails().
        dealerDetails(addressWithoutUprn)
      go to VehicleLookupPage

      click on back

      pageTitle should equal(BusinessChooseYourAddressPage.title)
    }

    "display EnterAddressManually page when back link is clicked " +
      "after user has manually entered the address" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      CookieFactoryForUISpecs.
        setupTradeDetails().
        dealerDetails(addressWithoutUprn).
        enterAddressManually()
      go to VehicleLookupPage

      click on back

      pageTitle should equal(EnterAddressManuallyPage.title)
    }
  }

  "exit button" should {
    "display before you start page" taggedAs UiTag in new WebBrowserForSelenium {
      go to BeforeYouStartPage
      cacheSetup()
      go to VehicleLookupPage

      click on exit

      pageTitle should equal(BeforeYouStartPage.title)
    }

    "remove redundant cookies" taggedAs UiTag in new PhantomJsByDefault {
      go to BeforeYouStartPage
      cacheSetup()
      go to VehicleLookupPage

      click on exit

      // Verify the cookies identified by the full set of cache keys have been removed
      AllCacheKeys.foreach(cacheKey => {
        webDriver.manage().getCookieNamed(cacheKey) should equal(null)
      })
    }
  }

  private def cacheSetup()(implicit webDriver: WebDriver) =
    CookieFactoryForUISpecs.
      setupTradeDetails().
      dealerDetails().
      disposeOccurred

  private val fakeAppWithHtml5ValidationDisabledConfig =
    LightFakeApplication(TestGlobalWithFilters, Map("html5Validation.enabled" -> false))

  private val fakeAppWithHtml5ValidationEnabledConfig =
    LightFakeApplication(TestGlobalWithFilters, Map("html5Validation.enabled" -> true))

}
