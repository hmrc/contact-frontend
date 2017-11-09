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
    "uk.gov.hmrc" %% "bootstrap-play-25" % "0.14.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.14.0",
    "uk.gov.hmrc" %% "play-ui" % "7.8.0",
    "uk.gov.hmrc" %% "url-builder" % "2.1.0",
    "uk.gov.hmrc" %% "auth-client" % "2.3.0",
    "commons-validator" % "commons-validator" % "1.6",
    "org.apache.httpcomponents" % "httpclient" % DependencyVersions.apacheHttpComponentsCore
  )

  val test = Seq(
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % "test",
    "org.mockito" % "mockito-all" % "2.0.2-beta" % "test",
    "org.pegdown" % "pegdown" % "1.6.0" % "test",
    "org.jsoup" % "jsoup" % "1.7.3" % "test",
    "com.github.tomakehurst" % "wiremock" % "1.58" % "test",
    "uk.gov.hmrc" %% "scala-webdriver" % "5.16.0" % "test",
    "uk.gov.hmrc" %% "hmrctest" % "2.3.0" % "test"
  ).map(_.exclude("org.seleniumhq.selenium", "selenium-api"))
    .map(_.exclude("org.seleniumhq.selenium", "selenium-java")) ++ Seq(
    "org.seleniumhq.selenium" % "selenium-java" % "2.53.1" % "test")

  def apply() = compile ++ test
}


