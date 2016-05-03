package composition

import com.google.inject.Key
import com.google.inject.name.Names
import com.typesafe.config.ConfigFactory
import java.io.File
import java.util.{TimeZone, Date, UUID}
import org.joda.time.DateTimeZone
import play.api.i18n.Lang
import play.api.mvc.Results.{NotFound,BadRequest}
import play.api.mvc.{RequestHeader, Result}
import play.api.Play.current
import play.api.{Application, Configuration, GlobalSettings, Logger, LoggerLike, Mode, Play}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.dvla.vehicles.presentation.common.filters.AccessLoggingFilter.AccessLoggerName
import uk.gov.dvla.vehicles.presentation.common.filters.ClfEntryBuilder
import utils.helpers.Config

/**
 * Application configuration is in a hierarchy of files:
 *
 *         application.conf
 * /             |            \
 * application.prod.conf  application.dev.conf  application.test.conf <- these can override and add to application.conf
 *
 * play test  <- test mode picks up application.test.conf
 * play run   <- dev mode picks up application.dev.conf
 * play start <- prod mode picks up application.prod.conf
 *
 * To override and stipulate a particular "conf" e.g.
 * play -Dconfig.file=conf/application.test.conf run
 */
trait GlobalWithFilters extends WithFilters with GlobalSettings with Composition {
  /**
   * Controllers must be resolved through the application context. There is a special method of GlobalSettings
   * that we can override to resolve a given controller. This resolution is required by the Play router.
   */
  override def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)

  override def onStart(app: Application) {
    Logger.info("vehicles-online Started") // used for operations, do not remove
    val localTimeZone = "Europe/London"
    TimeZone.setDefault(TimeZone.getTimeZone(localTimeZone))
    DateTimeZone.setDefault(DateTimeZone.forID(localTimeZone))
  }

  override def onLoadConfig(configuration: Configuration,
                            path: File,
                            classloader: ClassLoader,
                            mode: Mode.Mode): Configuration = {
    val dynamicConfig = Configuration.from(Map("session.cookieName" -> UUID.randomUUID().toString.substring(0, 16)))
    val applicationConf = System.getProperty("config.file", s"application.${mode.toString.toLowerCase}.conf")
    val environmentOverridingConfiguration = configuration ++
      Configuration(ConfigFactory.load(applicationConf)) ++
      dynamicConfig
    super.onLoadConfig(environmentOverridingConfiguration, path, classloader, mode)
  }

  override def onStop(app: Application) {
    super.onStop(app)
    Logger.info("vehicles-online Stopped") // used for operations, do not remove
  }

  // 404 - page not found error http://alvinalexander.com/scala/handling-scala-play-framework-2-404-500-errors
  override def onHandlerNotFound(request: RequestHeader): Future[Result] = Future.successful {
    val playLangCookie = request.cookies.get(Play.langCookieName)
    val value: String = playLangCookie match {
      case Some(cookie) => cookie.value
      case None => "en"
    }
    implicit val lang: Lang = Lang(value)
    implicit val config = injector.getInstance(classOf[Config])
    implicit val isPrivateKeeper = false
    Logger.warn(s"Broken link returning http code 404. uri: ${request.uri}")
    NotFound(views.html.errors.onHandlerNotFound(request))
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] =
    Future(errorStrategy(request, ex))

  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = Future.successful {
    implicit val config = injector.getInstance(classOf[Config])
    implicit val isPrivateKeeper = false

    val result = BadRequest(views.html.errors.onHandlerBadRequest(request, error))

    val clfEntryBuilder = injector.getInstance(classOf[ClfEntryBuilder])
    val accessLogger = injector.getInstance(Key.get(classOf[LoggerLike], Names.named(AccessLoggerName)))
    clfEntryBuilder.clfEntry(new Date(), request, result)(accessLogger)
    result
  }
}