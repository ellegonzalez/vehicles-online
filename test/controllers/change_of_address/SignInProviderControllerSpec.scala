package controllers.change_of_address

import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import controllers.change_of_address
import helpers.change_of_address.LoginPagePopulate
import helpers.UnitSpec

class SignInProviderUnitSpec extends UnitSpec {

  "SignInProvider - Controller" should {

    "present" in new WithApplication {
      val request = FakeRequest().withSession()

      val result = change_of_address.SignInProvider.present(request)

      status(result) should equal(OK)
    }

    "redirect to next page after the button is clicked" in new WithApplication {
      val request = FakeRequest().withSession()

      val result = change_of_address.SignInProvider.submit(request)

      status(result) should equal(SEE_OTHER)
      redirectLocation(result) should equal(Some(LoginPagePopulate.url))
    }
  }
}