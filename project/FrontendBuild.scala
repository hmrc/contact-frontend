import sbt._

object FrontendBuild extends Build with MicroService {
  import scala.util.Properties.envOrElse
  private val commonSettings = net.virtualvoid.sbt.graph.DependencyGraphSettings.graphSettings
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
    "uk.gov.hmrc" %% "bootstrap-play-25" % "1.2.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.14.0",
    "uk.gov.hmrc" %% "play-ui" % "7.13.0",
    "uk.gov.hmrc" %% "url-builder" % "2.1.0",
    "uk.gov.hmrc" %% "auth-client" % "2.5.0",
    "commons-validator" % "commons-validator" % "1.6",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0",
    "org.apache.httpcomponents" % "httpclient" % DependencyVersions.apacheHttpComponentsCore
  )

  val test = Seq(
    "org.scalatest" %% "scalatest" % "3.0.4" % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % "test",
    "org.mockito" % "mockito-all" % "2.0.2-beta" % "test",
    "org.pegdown" % "pegdown" % "1.6.0" % "test",
    "org.jsoup" % "jsoup" % "1.7.3" % "test",
    "com.github.tomakehurst" % "wiremock" % "1.58" % "test",
    "uk.gov.hmrc" %% "scala-webdriver" % "5.18.0",
    "uk.gov.hmrc" %% "hmrctest" % "3.0.0" % "test"
  ).map(_.exclude("org.seleniumhq.selenium", "selenium-api"))
    .map(_.exclude("org.seleniumhq.selenium", "selenium-java")) ++ Seq(
    "org.seleniumhq.selenium" % "selenium-java" % "3.7.1" % "test")

  def apply() = compile ++ test
}


