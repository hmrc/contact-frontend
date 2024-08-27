import sbt.*

object AppDependencies {

  def dependencies(testPhases: Seq[String]): Seq[ModuleID] = {
    val testDependencies = testPhases.flatMap(test)
    compile ++ testDependencies
  }

  private val bootstrapFrontendVersion = "9.3.0"
  private val playFrontendHmrcVersion  = "10.10.0"
  private val playVersion              = "play-30"

  private val compile = Seq(
    "uk.gov.hmrc" %% s"bootstrap-frontend-$playVersion" % bootstrapFrontendVersion,
    "uk.gov.hmrc" %% s"play-frontend-hmrc-$playVersion" % playFrontendHmrcVersion
  )

  private def test(scope: String) = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion" % bootstrapFrontendVersion % scope,
    "uk.gov.hmrc"       %% "ui-test-runner"               % "0.35.0"                 % scope,
    "org.scalatestplus" %% "selenium-4-21"                % "3.2.19.0"               % scope,
    "org.scalatestplus" %% "mockito-3-4"                  % "3.2.10.0"               % scope,
    "org.jsoup"          % "jsoup"                        % "1.17.2"                 % scope
  )
}
