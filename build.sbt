import sbt.*
import sbt.Keys.*
import uk.gov.hmrc.DefaultBuildSettings.*
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "tamc"

ThisBuild / scalaVersion := "3.6.4"
ThisBuild / majorVersion := 4
ThisBuild / scalafmtOnCompile := true

ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-Xfatal-warnings",
  "-Wconf:src=target/.*:s", // silence warnings from compiled files
  "-Wconf:src=routes/.*:s",
  "-Wconf:msg=Flag.*repeatedly:s",
  "-Wconf:msg=.*-Wunused.*:s"
)

val scoverageSettings: Seq[Def.Setting[?]] = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*Reverse.*;.*Routes.*;.*errors.*;.*models.*;.*PertaxAuthResponse.*",
    ScoverageKeys.coverageExcludedFiles := "<empty>;.*MarriageAllowanceConnector.*", //excluding traits that are overridden
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageMinimumBranchTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(
    defaultSettings(),
    scalaSettings,
    scoverageSettings,
    PlayKeys.playDefaultPort := 9909,
    retrieveManaged := true,
    libraryDependencies ++= AppDependencies.all,
    routesImport ++= Seq("binders.NinoPathBinder._", "uk.gov.hmrc.domain._")
  )
//  .settings(CodeCoverageSettings.settings *)

val it: Project = project.in(file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())
