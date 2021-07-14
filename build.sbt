import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "contact-frontend"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    scalaVersion := "2.12.10",
    majorVersion := 3,
    libraryDependencies ++= AppDependencies.dependencies(testPhases = Seq("test", "it")),
    resolvers += Resolver.mavenLocal
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest, AcceptanceTest)
  .settings(unitTestSettings, acceptanceTestSettings)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    PlayKeys.playDefaultPort := 9250,
    PlayKeys.devSettings ++= Seq("metrics.enabled" -> "false"),
    pipelineStages in Assets := Seq(gzip)
  )
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "config.AppConfig",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.govukfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._"
    ),
    // ***************
    // Use the silencer plugin to suppress warnings from unused imports in compiled twirl templates
    scalacOptions += "-P:silencer:pathFilters=views;routes",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.4.4" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.4.4" % Provided cross CrossVersion.full
    )
    // ***************
  )

lazy val unitTestSettings =
  inConfig(Test)(Defaults.testTasks) ++
    Seq(
      testOptions in Test := Seq(
        Tests.Filters(
          Seq(
            _ startsWith "connectors",
            _ startsWith "controllers",
            _ startsWith "helpers",
            _ startsWith "resources",
            _ startsWith "services",
            _ startsWith "util",
            _ startsWith "views"
          )
        )
      )
    )

lazy val AcceptanceTest = config("acceptance") extend Test

lazy val acceptanceTestSettings =
  inConfig(AcceptanceTest)(Defaults.testTasks) ++
    Seq(
      // The following is needed to preserve the -Dbrowser option to the HMRC webdriver factory library
      fork in AcceptanceTest := false,
      (testOptions in AcceptanceTest) := Seq(Tests.Filter(_ startsWith "acceptance")),
      AcceptanceTest / run / javaOptions ++= Seq(
        "-Dconfig.resource=test.application.conf",
        "-Dlogger.resource=logback-test.xml"
      )
    )
