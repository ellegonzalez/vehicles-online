package composition

import com.google.inject.util.Modules
import com.google.inject.{Guice, Injector, Module}
import com.tzavellas.sse.guice.ScalaModule
import org.scalatest.mock.MockitoSugar
import play.api.Logger
import uk.gov.dvla.vehicles.presentation.common
import common.clientsidesession.ClearTextClientSideSessionFactory
import common.clientsidesession.ClientSideSessionFactory
import common.clientsidesession.NoCookieFlags
import common.clientsidesession.CookieFlags
import common.services.DateService
import common.webserviceclients.addresslookup.{AddressLookupWebService, AddressLookupService}
import common.webserviceclients.bruteforceprevention.BruteForcePreventionWebService
import common.webserviceclients.vehicleandkeeperlookup.VehicleAndKeeperLookupWebService
import webserviceclients.dispose.{DisposeWebService, DisposeConfig}
import webserviceclients.dispose_service.FakeDisposeConfig
import webserviceclients.fakes.brute_force_protection.FakeBruteForcePreventionWebServiceImpl
import webserviceclients.fakes.FakeAddressLookupWebServiceImpl
import webserviceclients.fakes.FakeDateServiceImpl
import webserviceclients.fakes.FakeDisposeWebServiceImpl
import webserviceclients.fakes.FakeVehicleAndKeeperLookupWebService

trait TestComposition extends Composition {
  override lazy val injector: Injector = Guice.createInjector(testMod)

  private def testMod = Modules.`override`(new DevModule {
    override def bindSessionFactory() = ()
  }).`with`(new TestModule)

  def testModule(module: Module*) = Modules.`override`(testMod).`with`(module: _*)

  def testInjector(module: Module*) = Guice.createInjector(testModule(module: _*))
}

private class TestModule() extends ScalaModule with MockitoSugar {
  /**
   * Bind the fake implementations the traits
   */
  def configure() {
    Logger.debug("Guice is loading TestModule")

    bind[DisposeConfig].to[FakeDisposeConfig].asEagerSingleton()
    bind[utils.helpers.Config].toInstance(new TestConfig)

    ordnanceSurveyAddressLookup()
    bind[VehicleAndKeeperLookupWebService].to[FakeVehicleAndKeeperLookupWebService].asEagerSingleton()

    bind[DisposeWebService].to[FakeDisposeWebServiceImpl].asEagerSingleton()
    bind[DateService].to[FakeDateServiceImpl].asEagerSingleton()

    bind[CookieFlags].to[NoCookieFlags].asEagerSingleton()
    bind[ClientSideSessionFactory].to[ClearTextClientSideSessionFactory].asEagerSingleton()

    bind[BruteForcePreventionWebService].to[FakeBruteForcePreventionWebServiceImpl].asEagerSingleton()
  }


  private def ordnanceSurveyAddressLookup() = {
    bind[AddressLookupService]
      .to[uk.gov.dvla.vehicles.presentation.common.webserviceclients.addresslookup.ordnanceservey.AddressLookupServiceImpl]

    val fakeWebServiceImpl = new FakeAddressLookupWebServiceImpl(
      responseOfPostcodeWebService = FakeAddressLookupWebServiceImpl.responseValidForPostcodeToAddress
    )
    bind[AddressLookupWebService].toInstance(fakeWebServiceImpl)
  }
}
