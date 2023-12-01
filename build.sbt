import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "contact-frontend"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    scalaVersion := "2.13.8",
    majorVersion := 4,
    libraryDependencies ++= AppDependencies.dependencies(testPhases = Seq("test", "it")),
  )
  .configs(IntegrationTest, AcceptanceTest)
  .settings(unitTestSettings, acceptanceTestSettings)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    PlayKeys.playDefaultPort := 9250,
    PlayKeys.devSettings ++= Seq("metrics.enabled" -> "false"),
    Assets / pipelineStages := Seq(gzip)
  )
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "config.AppConfig",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._"
    ),
    // ***************
    // Use the silencer plugin to suppress warnings from unused imports in compiled twirl templates
    scalacOptions += "-P:silencer:pathFilters=views;routes",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.8" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.7.8" % Provided cross CrossVersion.full
    ),
    // ***************
    A11yTest / unmanagedSourceDirectories += (baseDirectory.value / "test" / "a11y"),
    resolvers += Resolver.mavenLocal
  )

lazy val unitTestSettings =
  inConfig(Test)(Defaults.testTasks) ++
    Seq(
      Test / testOptions := Seq(
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
      AcceptanceTest / fork := false,
      AcceptanceTest / testOptions := Seq(Tests.Filter(_ startsWith "acceptance")),
      AcceptanceTest / run / javaOptions ++= Seq(
        "-Dconfig.resource=test.application.conf",
        "-Dlogger.resource=logback-test.xml"
      )
    )
