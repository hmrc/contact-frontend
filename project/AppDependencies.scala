import sbt.*

object AppDependencies {

  private val bootstrapFrontendVersion = "9.11.0"
  private val playFrontendHmrcVersion  = "12.1.0"
  private val playVersion              = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% s"bootstrap-frontend-$playVersion" % bootstrapFrontendVersion,
    "uk.gov.hmrc" %% s"play-frontend-hmrc-$playVersion" % playFrontendHmrcVersion,
    "uk.gov.hmrc" %% s"domain-$playVersion"             % "12.1.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion" % bootstrapFrontendVersion % Test,
    "org.scalatestplus" %% "mockito-3-4"                  % "3.2.10.0"               % Test,
    "org.jsoup"          % "jsoup"                        % "1.17.2"                 % Test
  )
}
