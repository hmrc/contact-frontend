import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "contact-frontend"

// TODO: restrict parallelExecution to tests only (the obvious way to do this using Test scope does not seem to work correctly)
parallelExecution in Global := false

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(
    scalaVersion        := "2.12.10",
    majorVersion        := 3,
    libraryDependencies ++= AppDependencies.dependencies(testPhases = Seq("test", "it"))
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    PlayKeys.playDefaultPort := 9250
  )
  .settings(
    // ***************
    // Use the silencer plugin to suppress warnings from unused imports in compiled twirl templates
    scalacOptions += "-P:silencer:pathFilters=views;routes",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.4.4" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.4.4" % Provided cross CrossVersion.full
    )
    // ***************
  )

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork        := true,
  javaOptions ++= Seq(
    "-Dconfig.resource=test.application.conf",
    "-Dlogger.resource=logback-test.xml"
  )
)
