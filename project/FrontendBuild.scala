import sbt._

object FrontendBuild extends Build with MicroService {
  import scala.util.Properties.envOrElse

  val appName = "contact-frontend"
  val appVersion = envOrElse("CONTACT_FRONTEND_VERSION", "999-SNAPSHOT")

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  private val playHealthVersion = "0.7.0"
  private val playUiVersion = "1.8.0"
  private val govUkTemplateVersion =  "2.6.0"

  private val scalatestVersion = "2.2.2"
  private val pegdownVersion = "1.4.2"

  val compile = Seq(
    "uk.gov.hmrc" %% "frontend-wiring" % "0.1.0",
    "uk.gov.hmrc" %% "play-json-logger" % "1.0.0",
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "govuk-template" % govUkTemplateVersion,
    "uk.gov.hmrc" %% "url-builder" % "0.5.0",
    "org.apache.httpcomponents" % "httpclient" % "4.3.1"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest" %% "scalatest" % "2.2.2" % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope,
        "org.jsoup" % "jsoup" % "1.7.3" % scope,

        "com.github.tomakehurst" % "wiremock" % "1.48" % "test",
        "uk.gov.hmrc" %% "scala-webdriver" % "4.22.0" % "test",
        "uk.gov.hmrc" %% "hmrctest" % "1.0.0" % "test"
      )
    }.test
  }

  def apply() = compile ++ Test()
}


