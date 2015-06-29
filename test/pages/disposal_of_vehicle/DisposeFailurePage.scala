package pages.disposal_of_vehicle

import org.openqa.selenium.WebDriver
import pages.ApplicationContext.applicationContext
import uk.gov.dvla.vehicles.presentation.common.helpers
import helpers.webbrowser.{Element, Page, WebBrowserDSL, WebDriverFactory}
import views.disposal_of_vehicle.DisposeFailure
import DisposeFailure.{SetupTradeDetailsId, VehicleLookupId}

object DisposeFailurePage extends Page with WebBrowserDSL {
  final val address = s"$applicationContext/sell-to-the-trade-failure"
  final override val title: String = "Buying a vehicle into trade: failure"

  override def url: String = WebDriverFactory.testUrl + address.substring(1)

  def setuptradedetails(implicit driver: WebDriver): Element = find(id(SetupTradeDetailsId)).get

  def vehiclelookup(implicit driver: WebDriver): Element = find(id(VehicleLookupId)).get
}