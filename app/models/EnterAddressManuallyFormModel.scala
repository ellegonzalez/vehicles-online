package models

import uk.gov.dvla.vehicles.presentation.common.views.models.AddressAndPostcodeViewModel
import play.api.data.Forms.mapping
import play.api.libs.json.Json
import uk.gov.dvla.vehicles.presentation.common.clientsidesession.CacheKey

final case class EnterAddressManuallyFormModel(addressAndPostcodeModel: AddressAndPostcodeViewModel)

object EnterAddressManuallyFormModel {
  implicit val JsonFormat = Json.format[EnterAddressManuallyFormModel]

  final val EnterAddressManuallyCacheKey = "enterAddressManually"
  implicit val Key = CacheKey[EnterAddressManuallyFormModel](EnterAddressManuallyCacheKey)

  object Form {
    final val AddressAndPostcodeId = "addressAndPostcode"
    final val Mapping = mapping(
      AddressAndPostcodeId -> AddressAndPostcodeViewModel.Form.Mapping
    )(EnterAddressManuallyFormModel.apply)(EnterAddressManuallyFormModel.unapply)
  }
}
