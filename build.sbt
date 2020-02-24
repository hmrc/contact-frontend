import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "contact-frontend"

val apacheHttpComponentsCore = "4.4.1"

lazy val appOverrides: Set[ModuleID] = Set[ModuleID](
  "org.apache.httpcomponents" % "httpcore" % apacheHttpComponentsCore
)
lazy val plugins : Seq[Plugins] = Seq(
  play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory
)
lazy val playSettings : Seq[Setting[_]] = Seq.empty

def unitFilter(name: String): Boolean = !funFilter(name) && !smokeFilter(name)
def funFilter(name: String): Boolean = name startsWith "features"
def smokeFilter(name: String): Boolean = name startsWith "smoke"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins : _*)
  .settings(playSettings : _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(majorVersion := 3)
  .settings(
    PlayKeys.playDefaultPort := 9250,
    targetJvm := "jvm-1.8",
    libraryDependencies ++= compile ++ test("test") ++ test("it"),
    dependencyOverrides ++= Set[ModuleID](
      "org.apache.httpcomponents" % "httpcore" % apacheHttpComponentsCore
    ),
    parallelExecution in Test := false,
    fork in Test := false
  )
  .settings(testOptions in Test := Seq(Tests.Filter(unitFilter)),
    addTestReportOption(Test, "test-reports"),
    unmanagedSourceDirectories in FunTest <<= (baseDirectory in FunTest)(base => Seq(base / "test/unit")),
    unmanagedResourceDirectories in FunTest <<= (baseDirectory in FunTest)(base => Seq(base / "test/unit"))
  )
  .configs(IntegrationTest)
  .settings(inConfig(TemplateItTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "it")),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false)
  .configs(FunTest)
  .settings(inConfig(FunTest)(Defaults.testSettings): _*)
  .settings(
    testOptions in FunTest := Seq(Tests.Filter(funFilter)),
    unmanagedSourceDirectories   in FunTest <<= (baseDirectory in FunTest)(base => Seq(base / "test")),
    unmanagedResourceDirectories in FunTest <<= (baseDirectory in FunTest)(base => Seq(base / "test")),
    Keys.fork in FunTest := false,
    parallelExecution in FunTest := false,
    addTestReportOption(FunTest, "fun-test-reports")
  )
  .configs(SmokeTest)
  .settings(inConfig(SmokeTest)(Defaults.testSettings): _*)
  .settings(
    javaOptions in SmokeTest := Seq("-Denvironment=qa"),
    testOptions in SmokeTest := Seq(Tests.Filter(smokeFilter)),
    unmanagedSourceDirectories   in SmokeTest <<= (baseDirectory in SmokeTest)(base => Seq(base / "test")),
    unmanagedResourceDirectories in SmokeTest <<= (baseDirectory in SmokeTest)(base => Seq(base / "test")),
    Keys.fork in SmokeTest := true,
    parallelExecution in SmokeTest := false,
    addTestReportOption(SmokeTest, "smoke-test-reports")
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    resolvers += Resolver.bintrayRepo("hmrc", "releases"),
    resolvers += Resolver.jcenterRepo,
    resolvers += "HMRC private repository releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases"
  )

val compile = Seq(
  "uk.gov.hmrc"               %% "bootstrap-play-26" % "1.5.0",
  "uk.gov.hmrc"               %% "govuk-template"    % "5.52.0-play-26",
  "uk.gov.hmrc"               %% "play-ui"           % "8.8.0-play-26",
  "uk.gov.hmrc"               %% "url-builder"       % "3.3.0-play-26",
  "uk.gov.hmrc"               %% "auth-client"       % "2.33.0-play-26",
  "commons-validator"         %  "commons-validator" % "1.6",
  "org.apache.httpcomponents" %  "httpclient"        % apacheHttpComponentsCore
)

def test(scope: String) = Seq(
  "org.scalatest"           %% "scalatest"          % "3.0.1" % scope,
  "org.scalatestplus.play"  %% "scalatestplus-play" % "3.1.2" % scope,
  "com.typesafe.play"       %% "play-test"          % "2.6.0" % scope,
  "org.mockito"             %  "mockito-all"        % "2.0.2-beta" % scope,
  "org.pegdown"             %  "pegdown"            % "1.6.0" % scope,
  "org.jsoup"               %  "jsoup"              % "1.11.3" % scope,
  "com.github.tomakehurst"  %  "wiremock"           % "1.58" % scope,
  "uk.gov.hmrc"             %% "scala-webdriver"    % "7.9.0" % scope,
  "org.seleniumhq.selenium" % "selenium-java"       % "3.7.1" % scope,
  "uk.gov.hmrc"             %% "hmrctest"           % "3.3.0" % scope,
  "org.scalacheck"          %% "scalacheck"         % "1.14.0" % scope,
  "uk.gov.hmrc"             %% "bootstrap-play-26"  % "1.5.0" % scope classifier "tests"
)

val allPhases = "tt->test;test->test;test->compile;compile->compile"
val allItPhases = "tit->it;it->it;it->compile;compile->compile"

lazy val TemplateTest = config("tt") extend Test
lazy val TemplateItTest = config("tit") extend IntegrationTest
lazy val FunTest = config("fun") extend Test
lazy val SmokeTest = config("smoke") extend Test

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests map {
    test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }
