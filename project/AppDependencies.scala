
import sbt.*

object AppDependencies {

  private val hmrcBootstrapVersion = "10.8.0"
  private val playVersion = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% s"bootstrap-backend-$playVersion"  % hmrcBootstrapVersion,
    "uk.gov.hmrc"   %% s"domain-$playVersion"             % "13.0.0",
    "uk.gov.hmrc"   %% "tax-year"                         % "6.0.0",
    "org.typelevel" %% "cats-core"                        % "2.13.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"         %% s"bootstrap-test-$playVersion"    % hmrcBootstrapVersion,
    "org.scalatestplus"   %% "scalacheck-1-18"                 % "3.2.19.0",
    "uk.gov.hmrc"         %% s"domain-test-$playVersion"       % "13.0.0"
  ) map (_ % Test)

  val all: Seq[ModuleID] = compile ++ test
}
