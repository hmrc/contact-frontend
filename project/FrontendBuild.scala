import sbt._

object FrontendBuild extends Build with MicroService {
  import scala.util.Properties.envOrElse

  val appName = "contact-frontend"
  val appVersion = envOrElse("CONTACT_FRONTEND_VERSION", "999-SNAPSHOT")

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
  override lazy val appOverrides: Set[ModuleID] = AppOverrides()
}

object DependencyVersions {
  val apacheHttpComponentsCore = "4.4.1"
}

private object AppOverrides{
  val overrides = Set[ModuleID](
    "org.apache.httpcomponents" % "httpcore" % DependencyVersions.apacheHttpComponentsCore
  )

  def apply() = overrides
}

private object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap" % "6.7.0",
    "uk.gov.hmrc" %% "play-authorised-frontend" % "5.5.0",
    "uk.gov.hmrc" %% "play-config" % "2.0.1",
    "uk.gov.hmrc" %% "play-json-logger" % "2.1.0",
    "uk.gov.hmrc" %% "play-health" % "1.1.0",
    "uk.gov.hmrc" %% "play-ui" % "4.16.0",
    "uk.gov.hmrc" %% "govuk-template" % "4.0.0",
    "uk.gov.hmrc" %% "url-builder" % "1.0.0",
    "org.apache.httpcomponents" % "httpclient" % DependencyVersions.apacheHttpComponentsCore
  )

  val test = Seq(
    "org.scalatest" %% "scalatest" % "2.2.2" % "test",
    "org.scalatestplus" %% "play" % "1.2.0" % "test",
    "org.pegdown" % "pegdown" % "1.4.2" % "test",
    "org.jsoup" % "jsoup" % "1.7.3" % "test",
    "com.github.tomakehurst" % "wiremock" % "1.58" % "test",
    "uk.gov.hmrc" %% "scala-webdriver" % "5.12.0" % "test",
    "uk.gov.hmrc" %% "hmrctest" % "1.4.0" % "test"
  ).map(_.exclude("org.seleniumhq.selenium", "selenium-api"))
    .map(_.exclude("org.seleniumhq.selenium", "selenium-java")) ++ Seq(
    "org.seleniumhq.selenium" % "selenium-java" % "2.52.0" % "test")

  def apply() = compile ++ test
}


