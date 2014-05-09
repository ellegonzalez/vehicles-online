package controllers.disposal_of_vehicle

import helpers.UnitSpec
import mappings.common.AddressAndPostcode._
import mappings.common.AddressLines._
import mappings.common.Postcode._
import services.fakes.FakeAddressLookupService._

class EnterAddressManuallyFormSpec extends UnitSpec {

  "form" should {
    "accept if form is valid with all fields filled in" in {
      val model = formWithValidDefaults().get.addressAndPostcodeModel
      model.addressLinesModel.line1 should equal(line1Valid)
      model.addressLinesModel.line2 should equal(Some(line2Valid))
      model.addressLinesModel.line3 should equal(Some(line3Valid))
      model.addressLinesModel.line4 should equal(Some(line4Valid))
      model.postcode should equal(postcodeValid)
    }

    "accept if form is valid with only mandatory filled in" in {
      val model = formWithValidDefaults(line2 = "", line3 = "").get.addressAndPostcodeModel
      model.addressLinesModel.line1 should equal(line1Valid)
      model.postcode should equal(postcodeValid)
    }
  }

  "address lines" should {
    "accept if form address lines contain hyphens" in {
      val line1Hypthens = "1-1"
      val line2Hypthens = "address line - 2"
      val line3Hypthens = "address line - 3"
      val line4Hypthens = "address line - 4"
      val model = formWithValidDefaults(
        line1 = line1Hypthens,
        line2 = line2Hypthens,
        line3 = line3Hypthens,
        line4 = line4Hypthens).get.addressAndPostcodeModel

      model.addressLinesModel.line1 should equal(line1Hypthens)
      model.addressLinesModel.line2 should equal(Some(line2Hypthens))
      model.addressLinesModel.line3 should equal(Some(line3Hypthens))
      model.addressLinesModel.line4 should equal(Some(line4Hypthens))
    }

    "reject if line1 is blank" in {
      formWithValidDefaults(line1 = "").errors should have length 2
    }

    "reject if line1 is more than max length" in {
      formWithValidDefaults(line1 = "a" * (lineMaxLength + 1), line2 = "", line3 = "", line4 = "").errors should have length 1
    }

    "reject if line1 is greater than max length" in {
      formWithValidDefaults(line1 = "a" * (line1MaxLength + 1)).errors should have length 1
    }

    "reject if line1 contains special characters" in {
      formWithValidDefaults(line1 = "The*House").errors should have length 1
    }

    "reject if line2 is more than max length" in {
      formWithValidDefaults(line2 = "a" * (lineMaxLength + 1), line3 = "", line4 = "").errors should have length 1
    }

    "reject if line3 is more than max length" in {
      formWithValidDefaults(line2 = "", line3 = "a" * (lineMaxLength + 1), line4 = "").errors should have length 1
    }

    "reject if line4 is more than max length" in {
      formWithValidDefaults(line2 = "", line3 = "", line4 = "a" * (lineMaxLength + 1)).errors should have length 1
    }

    "reject if total length of all address lines is more than maxLengthOfLinesConcatenated" in {
      formWithValidDefaults(line1 = "a" * lineMaxLength,
        line2 = "b" * lineMaxLength,
        line3 = "c" * lineMaxLength,
        line4 = "d" * lineMaxLength
      ).errors should have length 1
    }

    "reject if any line contains html chevrons" in {
      formWithValidDefaults(line1 = "A<br>B").errors should have length 1
      formWithValidDefaults(line2 = "A<br>B").errors should have length 1
      formWithValidDefaults(line3 = "A<br>B").errors should have length 1
      formWithValidDefaults(line4 = "A<br>B").errors should have length 1
    }
  }

  "postcode" should {
    "reject if blank" in {
      formWithValidDefaults(postcode = "").errors should have length 3
    }

    "reject if less than min length" in {
      formWithValidDefaults(postcode = "SA99").errors should have length 2
    }

    "reject if contains special characters" in {
      formWithValidDefaults(postcode = "SA99 2L$").errors should have length 1
    }

    "reject if more than max length" in {
      formWithValidDefaults(postcode = "SA99 1DDR").errors should have length 2
    }

    "reject if contains html chevrons" in {
      formWithValidDefaults(postcode = "A<br>B").errors should have length 1
    }
  }

  private def formWithValidDefaults(line1: String = line1Valid,
                                    line2: String = line2Valid,
                                    line3: String = line3Valid,
                                    line4: String = line4Valid,
                                    postcode: String = postcodeValid) = {
    new EnterAddressManually().form.bind(
      Map(
        s"$addressAndPostcodeId.$addressLinesId.$line1Id" -> line1,
        s"$addressAndPostcodeId.$addressLinesId.$line2Id" -> line2,
        s"$addressAndPostcodeId.$addressLinesId.$line3Id" -> line3,
        s"$addressAndPostcodeId.$addressLinesId.$line4Id" -> line4,
        s"$addressAndPostcodeId.$postcodeId" -> postcode
      )
    )
  }
}
