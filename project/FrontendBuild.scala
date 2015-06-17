import sbt._

object FrontendBuild extends Build with MicroService {
  import scala.util.Properties.envOrElse

  val appName = "contact-frontend"
  val appVersion = envOrElse("CONTACT_FRONTEND_VERSION", "999-SNAPSHOT")

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  private val playHealthVersion = "0.7.0"
  private val playUiVersion = "1.8.1"
  private val govUkTemplateVersion =  "2.6.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap" % "0.5.1",
    "uk.gov.hmrc" %% "play-authorised-frontend" % "0.7.0",
    "uk.gov.hmrc" %% "play-config" % "1.0.0",
    "uk.gov.hmrc" %% "play-json-logger" % "1.0.0",
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "govuk-template" % govUkTemplateVersion,
    "uk.gov.hmrc" %% "url-builder" % "0.5.0",
    "org.apache.httpcomponents" % "httpclient" % "4.3.1"
  )

  val test = Seq(
    "org.scalatest" %% "scalatest" % "2.2.2" % "test",
    "org.scalatestplus" %% "play" % "1.2.0" % "test",
    "org.pegdown" % "pegdown" % "1.4.2" % "test",
    "org.jsoup" % "jsoup" % "1.7.3" % "test",
    "com.github.tomakehurst" % "wiremock" % "1.48" % "test",
    "uk.gov.hmrc" %% "scala-webdriver" % "4.22.0" % "test",
    "uk.gov.hmrc" %% "hmrctest" % "1.0.0" % "test"
  )

  def apply() = compile ++ test
}


