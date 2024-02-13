import sbt._

object AppDependencies {

  def dependencies(testPhases: Seq[String]): Seq[ModuleID] = {
    val testDependencies = testPhases.flatMap(test)
    compile ++ testDependencies
  }

  private val bootstrapFrontendVersion = "8.1.0"
  private val playFrontendHmrcVersion  = "8.5.0"
  private val playVersion              = "play-30"

  private val compile = Seq(
    "uk.gov.hmrc" %% s"bootstrap-frontend-$playVersion" % bootstrapFrontendVersion,
    "uk.gov.hmrc" %% s"play-frontend-hmrc-$playVersion" % playFrontendHmrcVersion
  )

  private def test(scope: String) = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion" % bootstrapFrontendVersion % scope,
    "uk.gov.hmrc"       %% "webdriver-factory"            % "0.46.0"                 % scope,
    "org.scalatestplus" %% "selenium-4-2"                 % "3.2.13.0"               % scope,
    "org.mockito"       %% "mockito-scala-scalatest"      % "1.16.37"                % scope,
    "org.jsoup"          % "jsoup"                        % "1.11.3"                 % scope
  )
}
