import sbt._

object FrontendBuild extends Build with MicroService {
  import scala.util.Properties.envOrElse
  private val commonSettings = net.virtualvoid.sbt.graph.DependencyGraphSettings.graphSettings
  val appName = "contact-frontend"

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
    "uk.gov.hmrc" %% "bootstrap-play-25" % "1.7.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.14.0",
    "uk.gov.hmrc" %% "play-ui" % "7.14.0",
    "uk.gov.hmrc" %% "url-builder" % "2.1.0",
    "uk.gov.hmrc" %% "auth-client" % "2.6.0",
    "commons-validator" % "commons-validator" % "1.6",
    "org.apache.httpcomponents" % "httpclient" % DependencyVersions.apacheHttpComponentsCore
  )

  def test(scope: String) = Seq(
    "org.scalatest" %% "scalatest" % "3.0.4" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
    "org.mockito" % "mockito-all" % "2.0.2-beta" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "org.jsoup" % "jsoup" % "1.7.3" % scope,
    "com.github.tomakehurst" % "wiremock" % "1.58" % scope,
    "uk.gov.hmrc" %% "scala-webdriver" % "5.23.0" %scope,
    "org.seleniumhq.selenium" % "selenium-java" % "3.7.1" % scope,
    "uk.gov.hmrc" %% "hmrctest" % "3.0.0" % scope
  )

  def apply() = compile ++ test("test") ++ test("it")
}


