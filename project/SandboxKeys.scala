import sbt.{Project, settingKey, taskKey}

object SandboxKeys {
  lazy val portOffset = settingKey[Int]("The port offset for all the microservices.")
  lazy val gatlingSimulation = settingKey[String]("The full class name of the Gatling simulation to be run.")
  lazy val acceptanceTests = taskKey[Unit]("A task for running the acceptance tests of the project.")
  lazy val runAllMicroservices = taskKey[Unit]("A task for running all the microservices need by the sandbox.")

  lazy val sandbox = taskKey[Unit]("Runs the whole sandbox for manual testing including microservices, webapp and legacy stubs'")
  lazy val sandboxAsync = taskKey[Unit]("Runs the whole sandbox asynchronously for manual testing including microservices, webapp and legacy stubs")
  lazy val gatling = taskKey[Unit]("Runs the gatling tests against the sandbox")
  lazy val accept = taskKey[Unit]("Runs all the acceptance tests against the sandbox.")
}
