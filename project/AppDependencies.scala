import sbt.*

object AppDependencies {

  private val bootstrapFrontendVersion = "9.4.0"
  private val playFrontendHmrcVersion  = "10.10.0"
  private val playVersion              = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% s"bootstrap-frontend-$playVersion" % bootstrapFrontendVersion,
    "uk.gov.hmrc" %% s"play-frontend-hmrc-$playVersion" % playFrontendHmrcVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion" % bootstrapFrontendVersion % Test,
    "org.scalatestplus" %% "mockito-3-4"                  % "3.2.10.0"               % Test,
    "org.jsoup"          % "jsoup"                        % "1.17.2"                 % Test
  )
}
