package models.domain.disposal_of_vehicle

import helpers.UnitSpec
import play.api.libs.json.Json
import uk.gov.dvla.vehicles.presentation.common.model.{AddressModel, VmAddressModel}
import uk.gov.dvla.vehicles.presentation.common.model.AddressModel.JsonFormat
import uk.gov.dvla.vehicles.presentation.common.views.models.{AddressAndPostcodeViewModel, AddressLinesViewModel}
import webserviceclients.fakes.FakeAddressLookupService.BuildingNameOrNumberValid
import webserviceclients.fakes.FakeAddressLookupService.Line2Valid
import webserviceclients.fakes.FakeAddressLookupService.Line3Valid
import webserviceclients.fakes.FakeAddressLookupService.PostTownValid
import webserviceclients.fakes.FakeAddressLookupService.PostcodeValid

final class AddressViewModelSpec extends UnitSpec {
  "from" should {
    "translate correctly" in {
      val addressAndPostcodeModel = AddressAndPostcodeViewModel(
        addressLinesModel = AddressLinesViewModel(
          buildingNameOrNumber = BuildingNameOrNumberValid,
          line2 = Some(Line2Valid),
          line3 = Some(Line3Valid),
          postTown = PostTownValid
        ),
        postCode = PostcodeValid
      )

      val result = VmAddressModel.from(addressAndPostcodeModel)

      result.address should equal(Seq(
        BuildingNameOrNumberValid.toUpperCase,
        Line2Valid.toUpperCase,
        Line3Valid.toUpperCase,
        PostTownValid.toUpperCase,
        PostcodeValid.toUpperCase))
    }
  }

  "format" should {
    "serialize to json" in {
      AddressModel(
        address = Seq(BuildingNameOrNumberValid, Line2Valid, Line3Valid, PostTownValid, PostcodeValid))
    }

    "deserialize from json" in {
      val fromJson =  Json.fromJson[AddressModel](asJson)
      val expected = AddressModel(
        address = Seq(BuildingNameOrNumberValid, Line2Valid, Line3Valid, PostTownValid, PostcodeValid))

      fromJson.asOpt should equal(Some(expected))
    }
  }

  private val asJson = Json.parse(
    s"""{"uprn":10123456789,"address":["12345ABCD","line2 stub","line3 stub","postTown stub","$PostcodeValid"]}""")
}
