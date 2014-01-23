import play.api.Play
import scala.util.{Success, Try}

package object app {

  object AccountStatus {
    val BankBuildingAccount = 'bankBuildingAccount
    val AppliedForAccount = 'appliedForAccount
    val NotOpenAccount = 'notOpenAccount
  }

  object ActingType {
    val Guardian = 'guardian
    val Attorney = 'attorney
    val Appointee = 'appointee
    val Judicial = 'judicial
    val Deputy = 'deputy
    val Curator = 'curator
  }

  object Whereabouts {
    val Home = "Home"
    val Hospital = "Hospital"
    val Holiday = "Holiday"
    val RespiteCare = "Respite Care"
    val CareHome = "Care Home"
    val NursingHome = "Nursing Home"
    val Other = "Other"
  }

  object PaymentFrequency {
    val EveryWeek = 'everyWeek
    val FourWeekly = 'fourWeekly
  }

  object PensionPaymentFrequency {
    val Weekly = "02"
    val Fortnightly = "03"
    val FourWeekly = "04"
    val Monthly = "05"
    val Other = "other" // TODO [SKW] the xsd is inconsistent and needs changing as there is no value for other, so I just made up a value and Jorge will change the schema and can replace this with a sensible value.
/*
    def mapToHumanReadableString(code: models.PensionPaymentFrequency): String = {
      mapToHumanReadableString(code.frequency)
    }
*/
    def mapToHumanReadableString(code: String): String = {
      code match {
        case Weekly => "Weekly"
        case Fortnightly => "Fortnightly"
        case FourWeekly => "Four-weekly"
        case Monthly => "Monthly"
        case Other => "Other"
        case _ => ""
      }
    }
  }

  object StatutoryPaymentFrequency {
    val Weekly = "weekly"
    val Fortnightly = "fortnightly"
    val FourWeekly = "fourWeekly"
    val Monthly = "monthly"
    val Other = "other"

    def mapToHumanReadableString(frequencyCode: String, otherCode: Option[String]): String = frequencyCode match {
      case Weekly => "Weekly"
      case Fortnightly => "Fortnightly"
      case FourWeekly => "Four-weekly"
      case Monthly => "Monthly"
      case Other => otherCode match {
        case Some(s) => "Other: " + s
        case _ => "Other"
      } //+ paymentFrequency.other.getOrElse("")
      case _ => ""
    }
/*
    def mapToHumanReadableString(paymentFrequencyOption: Option[models.PaymentFrequency]): String = paymentFrequencyOption match {
      case Some(s) => mapToHumanReadableString(s.frequency,None)
      case _ => ""
    }

    def mapToHumanReadableStringWithOther(paymentFrequencyOption: Option[models.PaymentFrequency]): String = paymentFrequencyOption match {
      case Some(s) => mapToHumanReadableString(s.frequency,s.other)
      case _ => ""
    }*/
  }

  object XMLValues {
    val NotAsked = "Not asked"
    val NotKnown = "Not known"
    val Yes = "Yes"
    val No = "No"
    val yes = "yes"
    val no = "no"
    val GBP = "GBP"
    val Other = "Other"
  }

  object WhoseNameAccount {
    val YourName = 'yourName
    val Yourpartner = 'partner
    val Both = 'bothNames
    val PersonActingBehalf = 'onBehalfOfYou
    val YouPersonBehalf = 'allNames
  }

  val mb = 131072
  def convertToMB(bytes:Long) = {
    bytes / mb
  }

  def convertToBytes(megaBytes:Long) = {
    megaBytes * mb
  }

  object ConfigProperties  {
    def getProperty(property:String,default:Int) = Try(Play.current.configuration.getInt(property).getOrElse(default)) match { case Success(s) => s case _ => default}
    def getProperty(property:String,default:String) = Try(Play.current.configuration.getString(property).getOrElse(default)) match { case Success(s) => s case _ => default}
    def getProperty(property:String,default:Boolean) = Try(Play.current.configuration.getBoolean(property).getOrElse(default)) match { case Success(s) => s case _ => default}
    def getProperty(property:String,default:Long) = Try(Play.current.configuration.getLong(property).getOrElse(default)) match { case Success(s) => s case _ => default}
  }
}