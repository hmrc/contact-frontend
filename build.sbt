import uk.gov.hmrc.DefaultBuildSettings.addTestReportOption
import play.sbt.routes.RoutesKeys

val appName = "contact-frontend"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    scalaVersion := "3.3.3",
    majorVersion := 4,
    libraryDependencies ++= AppDependencies.dependencies(testPhases = Seq("test", "it"))
  )
  .configs(IntegrationTest, AcceptanceTest)
  .settings(unitTestSettings, acceptanceTestSettings, integrationTestSettings)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    PlayKeys.playDefaultPort := 9250,
    PlayKeys.devSettings ++= Seq("metrics.enabled" -> "false"),
    Assets / pipelineStages := Seq(gzip)
  )
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "config.AppConfig",
      "uk.gov.hmrc.govukfrontend.views.html.components.*",
      "uk.gov.hmrc.hmrcfrontend.views.html.components.*"
    ),
    RoutesKeys.routesImport += "model.Aliases.*",
    A11yTest / unmanagedSourceDirectories += (baseDirectory.value / "test" / "a11y")
  )
  .settings(
    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions += "-Wconf:src=views/.*:s"
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

lazy val IntegrationTest         = config("it") extend Test
lazy val integrationTestSettings =
  inConfig(IntegrationTest)(Defaults.testTasks) ++
    Seq(
      (IntegrationTest / testOptions) := Seq(Tests.Filter(_ startsWith "it")),
      addTestReportOption(IntegrationTest, "it-test-reports")
    )
