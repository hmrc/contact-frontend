import sbt._

object AppDependencies {

  def dependencies(testPhases: Seq[String]): Seq[ModuleID] = {
    val testDependencies = testPhases.flatMap(test)
    compile ++ testDependencies
  }

  private val bootstrapFrontendVersion = "8.1.0"
  private val playFrontendHmrcVersion  = "8.6.0-SNAPSHOT"
//  private val playFrontendHmrcVersion  = "8.5.0"
  private val playVersion              = "play-30"

  private val compile = Seq(
    "uk.gov.hmrc" %% s"bootstrap-frontend-$playVersion" % bootstrapFrontendVersion,
    "uk.gov.hmrc" %% s"play-frontend-hmrc-$playVersion" % playFrontendHmrcVersion
  )

  private def test(scope: String) = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion" % bootstrapFrontendVersion % scope,
    "uk.gov.hmrc"       %% "ui-test-runner"               % "0.17.0"                 % scope,
    "org.scalatestplus" %% "selenium-4-12"                % "3.2.17.0"               % scope,
    "org.mockito"       %% "mockito-scala-scalatest"      % "1.16.37"                % scope,
    "org.jsoup"          % "jsoup"                        % "1.11.3"                 % scope
  )
}
