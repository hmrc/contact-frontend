import sbt._

object FrontendBuild extends Build with MicroService {
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
    "uk.gov.hmrc" %% "bootstrap-play-25" % "4.9.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.23.0",
    "uk.gov.hmrc" %% "play-ui" % "7.25.0-play-25",
    "uk.gov.hmrc" %% "url-builder" % "3.0.0",
    "uk.gov.hmrc" %% "auth-client" % "2.18.0-play-25",
    "commons-validator" % "commons-validator" % "1.6",
    "org.apache.httpcomponents" % "httpclient" % DependencyVersions.apacheHttpComponentsCore
  )

  def test(scope: String) = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
    "org.mockito" % "mockito-all" % "2.0.2-beta" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "org.jsoup" % "jsoup" % "1.11.3" % scope,
    "com.github.tomakehurst" % "wiremock" % "1.58" % scope,
    "uk.gov.hmrc" %% "scala-webdriver" % "7.9.0" %scope,
    "org.seleniumhq.selenium" % "selenium-java" % "3.7.1" % scope,
    "uk.gov.hmrc" %% "hmrctest" % "3.3.0" % scope,
    "org.scalacheck" %% "scalacheck" % "1.14.0" % "test"
  )

  def apply() = compile ++ test("test") ++ test("it")
}


