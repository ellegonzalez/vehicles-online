package controllers

import composition.WithApplication
import helpers.UnitSpec
import models.DisposeFormModel.Form.{ConsentId, DateOfDisposalId, LossOfRegistrationConsentId, MileageId, EmailOptionId}
import org.joda.time.{Instant, LocalDate}
import org.mockito.invocation.InvocationOnMock
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.when
import org.mockito.stubbing.Answer
import play.api.libs.json.Json
import uk.gov.dvla.vehicles.presentation.common.mappings.Email._
import uk.gov.dvla.vehicles.presentation.common.model.PrivateKeeperDetailsFormModel.Form.EmailId
import uk.gov.dvla.vehicles.presentation.common.model.PrivateKeeperDetailsFormModel.Form._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.dvla.vehicles.presentation.common.clientsidesession.{TrackingId, ClientSideSessionFactory}
import uk.gov.dvla.vehicles.presentation.common.mappings.DayMonthYear.{DayId, MonthId, YearId}
import uk.gov.dvla.vehicles.presentation.common.mappings.{OptionalToggle, Mileage}
import uk.gov.dvla.vehicles.presentation.common.services.DateService
import uk.gov.dvla.vehicles.presentation.common.views.models.DayMonthYear
import uk.gov.dvla.vehicles.presentation.common.webserviceclients.healthstats.HealthStats
import utils.helpers.Config
import webserviceclients.emailservice.{EmailService, EmailServiceSendRequest, EmailServiceSendResponse}
import webserviceclients.dispose.{DisposeConfig, DisposeRequestDto, DisposeServiceImpl, DisposeWebService}
import webserviceclients.fakes.FakeDateServiceImpl.{DateOfDisposalDayValid, DateOfDisposalMonthValid, DateOfDisposalYearValid}
import webserviceclients.fakes.FakeDisposeWebServiceImpl.{ConsentValid, MileageValid, disposeResponseSuccess}
import webserviceclients.fakes.FakeResponse

class DisposeFormSpec extends UnitSpec {
  "form" should {
    "accept when all fields contain valid responses" in new WithApplication {
      val model = formWithValidDefaults().get

      model.mileage.get should equal(MileageValid.toInt)
      model.dateOfDisposal should equal(
        new LocalDate(DateOfDisposalYearValid.toInt,
          DateOfDisposalMonthValid.toInt,
          DateOfDisposalDayValid.toInt)
      )
      model.consent should equal(ConsentValid)
      model.lossOfRegistrationConsent should equal(ConsentValid)
    }

    "accept when all mandatory fields contain valid responses" in new WithApplication {
      val model = formWithValidDefaults(
        mileage = "",
        dayOfDispose = DateOfDisposalDayValid,
        monthOfDispose = DateOfDisposalMonthValid,
        yearOfDispose = DateOfDisposalYearValid).get

      model.mileage should equal(None)
      model.dateOfDisposal should equal(
        new LocalDate(DateOfDisposalYearValid.toInt,
          DateOfDisposalMonthValid.toInt,
          DateOfDisposalDayValid.toInt)
      )
    }
  }

  "mileage" should {
    "reject if mileage is more than maximum" in new WithApplication {
      formWithValidDefaults(mileage = (Mileage.Max + 1).toString).errors should have length 1
    }
    "reject if mileage is not numeric" in new WithApplication {
      formWithValidDefaults(mileage = "Boom").errors should have length 1
    }
  }

  "dateOfDisposal" should {
    "reject if date day is not selected" in new WithApplication {
      formWithValidDefaults(dayOfDispose = "").errors should have length 1
    }

    "reject if date month is not selected" in new WithApplication {
      formWithValidDefaults(monthOfDispose = "").errors should have length 1
    }

    "reject if date year is not selected" in new WithApplication {
      formWithValidDefaults(yearOfDispose = "").errors should have length 1
    }

    // TODO : Fix and reinstate this test
//    "reject if date is in the future" in new WithApplication {
//      val dayToday: Int = DateOfDisposalDayValid.toInt
//      val dayOfDispose = (dayToday + 1).toString
//
//      // Attempting to dispose with a date 1 day into the future.
//      val result = formWithValidDefaults(
//        dayOfDispose = dayOfDispose)
//
//      result.errors should have length 1
//      result.errors(0).key should equal(DateOfDisposalId)
//      result.errors(0).message should equal("error.notInFuture")
//    }

    "reject if date is more than 2 years in the past" in new WithApplication {
      val dayToday: Int = DateOfDisposalDayValid.toInt
      val yearToday: Int = DateOfDisposalYearValid.toInt
      val dayOfDispose = (dayToday - 1).toString
      val yearOfDispose = (yearToday - 2).toString

      // Attempting to dispose with a date 2 years and 1 day into the past.
      val result = formWithValidDefaults(
        dayOfDispose = dayOfDispose,
        yearOfDispose = yearOfDispose)

      result.errors should have length 1
      result.errors.head.key should equal(DateOfDisposalId)
      result.errors.head.message should equal("error.date.notBefore")
    }

    "reject if date is too far in the past" in new WithApplication {
      val yearOfDispose = "1"
      val dateServiceStubbed = dateServiceStub(yearToday = 1)

      // Attempting to dispose with a date 2 years and 1 day into the past.
      val result = formWithValidDefaults(yearOfDispose = yearOfDispose,
        disposeController = dispose(dateServiceStubbed))

      result.errors should have length 1
      result.errors.head.key should equal(DateOfDisposalId)
      result.errors.head.message should equal("error.date.invalid")
    }

    "reject if date entered is an invalid date" in new WithApplication {
      val day = "31"
      val month = "2"
      val year = DateOfDisposalYearValid

      // Attempting to dispose with an invalid date.
      val result = formWithValidDefaults(
        dayOfDispose = day,
        monthOfDispose = month,
        yearOfDispose = year)

      result.errors should have length 1
      result.errors.head.key should equal(DateOfDisposalId)
      result.errors.head.message should equal("error.date.invalid")
    }
  }

  "consent" should {
    "reject if consent is not ticked" in new WithApplication {
      formWithValidDefaults(consent = "").errors should have length 1
    }
  }

  "lossOfRegistrationConsent" should {
    "reject if loss of registration consent is not ticked" in new WithApplication {
      formWithValidDefaults(lossOfRegistrationConsent = "").errors should have length 1
    }
  }

  private def dateServiceStub(dayToday: Int = DateOfDisposalDayValid.toInt,
                              monthToday: Int = DateOfDisposalMonthValid.toInt,
                              yearToday: Int = DateOfDisposalYearValid.toInt) = {
    val dayMonthYearStub = new DayMonthYear(day = dayToday,
      month = monthToday,
      year = yearToday)
    val dateService = mock[DateService]
    when(dateService.today).thenReturn(dayMonthYearStub)
    when(dateService.now).thenReturn(new Instant())
    dateService
  }

  private def dispose(dateService: DateService = dateServiceStub()) = {
    val ws = mock[DisposeWebService]
    when(ws.callDisposeService(any[DisposeRequestDto], any[TrackingId])).thenReturn(Future.successful {
      val responseAsJson = Json.toJson(disposeResponseSuccess)
      import play.api.http.Status.OK
      new FakeResponse(status = OK, fakeJson = Some(responseAsJson)) // Any call to a webservice will always return this successful response.
    })
    val healthStatsMock = mock[HealthStats]
    when(healthStatsMock.report(anyString)(any[Future[_]])).thenAnswer(new Answer[Future[_]] {
      override def answer(invocation: InvocationOnMock): Future[_] = invocation.getArguments()(1).asInstanceOf[Future[_]]
    })
    val disposeServiceImpl = new DisposeServiceImpl(new DisposeConfig(), ws, healthStatsMock, dateServiceStub())
    implicit val clientSideSessionFactory = injector.getInstance(classOf[ClientSideSessionFactory])

    val emailServiceMock: EmailService = mock[EmailService]
    when(emailServiceMock.invoke(any[EmailServiceSendRequest](), any[TrackingId])).
      thenReturn(Future(EmailServiceSendResponse()))

    implicit val config: Config = mock[Config]
    new Dispose(disposeServiceImpl, emailServiceMock, dateService)
  }

  private def formWithValidDefaults(mileage: String = MileageValid,
                                    dayOfDispose: String = DateOfDisposalDayValid,
                                    monthOfDispose: String = DateOfDisposalMonthValid,
                                    yearOfDispose: String = DateOfDisposalYearValid,
                                    consent: String = ConsentValid,
                                    lossOfRegistrationConsent: String = ConsentValid,
                                    disposeController: Dispose = dispose()) = {

    disposeController.form.bind(
      Map(
        MileageId -> mileage,
        s"$DateOfDisposalId.$DayId" -> dayOfDispose,
        s"$DateOfDisposalId.$MonthId" -> monthOfDispose,
        s"$DateOfDisposalId.$YearId" -> yearOfDispose,
        s"$EmailOptionId" -> OptionalToggle.Invisible,
        ConsentId -> consent,
        LossOfRegistrationConsentId -> lossOfRegistrationConsent
      )
    )
  }
}
