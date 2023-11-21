import sbt._

object AppDependencies {

  def dependencies(testPhases: Seq[String]): Seq[ModuleID] = {
    val testDependencies = testPhases.flatMap(test)
    compile ++ testDependencies
  }

  private val bootstrapPlayVersion = "7.22.0"

  private val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc"         % "7.29.0-play-28",
    "uk.gov.hmrc" %% "govuk-template"             % "5.80.0-play-28",
    "uk.gov.hmrc" %% "play-ui"                    % "9.12.0-play-28"
  )

  private def test(scope: String) = Seq(
    "uk.gov.hmrc"           %% "bootstrap-test-play-28"  % bootstrapPlayVersion % scope,
    "uk.gov.hmrc"           %% "domain"                  % "8.3.0-play-28"      % scope,
    "uk.gov.hmrc"           %% "webdriver-factory"       % "0.41.0"             % scope,
    "org.scalatestplus"     %% "scalatestplus-mockito"   % "1.0.0-M2"           % scope,
    "org.scalatestplus"     %% "selenium-4-2"            % "3.2.13.0"           % scope,
    "org.mockito"           %% "mockito-scala-scalatest" % "1.16.37"            % scope,
    "org.jsoup"              % "jsoup"                   % "1.11.3"             % scope,
    "com.github.tomakehurst" % "wiremock"                % "1.58"               % scope,
    "com.github.tomakehurst" % "wiremock-jre8"           % "2.27.2"             % scope
  )
}
