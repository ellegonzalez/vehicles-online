package models.domain.disposal_of_vehicle

import models.domain.common.{CacheKey, AddressLinesModel, AddressAndPostcodeModel}
import scala.annotation.tailrec
import play.api.libs.json.Json
import mappings.disposal_of_vehicle.EnterAddressManually.EnterAddressManuallyCacheKey

case class EnterAddressManuallyModel(addressAndPostcodeModel: AddressAndPostcodeModel) {
  def stripCharsNotAccepted = {
    @tailrec
    def stripEndOfLine(inputLine: Option[String]): Option[String] = inputLine match {
      case Some(line) =>
        val whitelist = """^[A-Za-z0-9\s\-]*$""".r
        line.takeRight(1) match {
          case lastChar if !whitelist.pattern.matcher(lastChar).matches => stripEndOfLine(Some(line.dropRight(1)))
          case _ => inputLine
        }
      case _ => None
    }

    val line1 = stripEndOfLine(Some(addressAndPostcodeModel.addressLinesModel.line1))
    val line2 = stripEndOfLine(addressAndPostcodeModel.addressLinesModel.line2)
    val line3 = stripEndOfLine(addressAndPostcodeModel.addressLinesModel.line3)
    val line4 = stripEndOfLine(addressAndPostcodeModel.addressLinesModel.line4)

    require(line1.isDefined, "Address line1 must have content")
    copy(addressAndPostcodeModel = addressAndPostcodeModel.copy(addressLinesModel = AddressLinesModel(line1.get, line2, line3, line4)))
  }
}

object EnterAddressManuallyModel {
  implicit final val JsonFormat = Json.format[EnterAddressManuallyModel]
  implicit final val Key = CacheKey[EnterAddressManuallyModel](EnterAddressManuallyCacheKey)
}
