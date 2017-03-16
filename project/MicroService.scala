import sbt.Keys._
import sbt.Tests.{SubProcess, Group}
import sbt._
import play.sbt.routes.RoutesKeys.routesGenerator

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import uk.gov.hmrc.{SbtBuildInfo, ShellPrompt}
  import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

  import TestPhases._

  val appName: String
  val appVersion: String

  lazy val appDependencies : Seq[ModuleID] = ???
  lazy val appOverrides: Set[ModuleID] = ???
  lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala)
  lazy val playSettings : Seq[Setting[_]] = Seq.empty

  def unitFilter(name: String): Boolean = !funFilter(name) && !smokeFilter(name)
  def funFilter(name: String): Boolean = name startsWith "features"
  def smokeFilter(name: String): Boolean = name startsWith "smoke"

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(plugins : _*)
    .settings(playSettings : _*)
    .settings(version := appVersion)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      targetJvm := "jvm-1.8",
      shellPrompt := ShellPrompt(appVersion),
      libraryDependencies ++= appDependencies,
      dependencyOverrides ++= appOverrides,
      parallelExecution in Test := false,
      fork in Test := false,
      retrieveManaged := true,
      routesGenerator := StaticRoutesGenerator
    )
    .settings(Repositories.playPublishingSettings : _*)
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
    .settings(SbtBuildInfo(): _*)
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
      resolvers += Resolver.jcenterRepo
    )
}

private object TestPhases {

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
}

private object Repositories {

  import uk.gov.hmrc._
  import PublishingSettings._
  import NexusPublishing._

  lazy val playPublishingSettings : Seq[sbt.Setting[_]] = sbtrelease.ReleasePlugin.releaseSettings ++ Seq(

    credentials += SbtCredentials,

    publishArtifact in(Compile, packageDoc) := false,
    publishArtifact in(Compile, packageSrc) := false
  ) ++
    publishAllArtefacts ++
    nexusPublishingSettings
}
