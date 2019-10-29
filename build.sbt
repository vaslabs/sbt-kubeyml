import xerial.sbt.Sonatype.GitHubHosting

name := "sbt-kubeyml"

version := "0.1.5"

scalaVersion := "2.12.10"

lazy val scala212 = "2.12.10"

lazy val `kubeyml` = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin, SbtPlugin)
  .settings(pluginSettings)
  .settings(
    name := "sbt-kubeyml"
  ).settings(publishSettings)
  .settings(releaseSettings)
  .settings(pluginSettings)


crossScalaVersions := Seq(scala212)

val circeVersion = "0.12.1"
credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credential")

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-yaml" % "0.10.0",
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.1" % Test
)
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.4.1")

lazy val pluginSettings = Seq(
  sbtPlugin := true,
  crossSbtVersions := Seq("1.2.8", "1.3.3")
)

lazy val publishSettings = Seq(
  publishTo := Some(
    if (isSnapshot.value) Opts.resolver.sonatypeSnapshots
    else Opts.resolver.sonatypeStaging
  ),
  organization := "org.vaslabs.kube",
  organizationName := "Vasilis Nicolaou",
  sonatypeProfileName := "org.vaslabs",
  sonatypeProjectHosting := Some(GitHubHosting("vaslabs", "sbt-kubeyml", "vaslabsco@gmail.com")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/vaslabs/sbt-kubeyml"),
    "scm:git@github.com:vaslabs/sbt-kubeyml.git")),
  developers := List(
    Developer(
      id    = "vaslabs",
      name  = "Vasilis Nicolaou",
      email = "vaslabsco@gmail.com",
      url   = url("http://vaslabs.org")
    )
  ),
  publishMavenStyle := true,
  licenses := List("MIT" -> new URL("https://opensource.org/licenses/MIT")),
  homepage := Some(url("https://git.vaslabs.org/vaslabs/sbt-kubeyml")),
  startYear := Some(2019)
)

lazy val releaseSettings = {
  import ReleaseTransformations._

  Seq(
    releaseCrossBuild := true,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      releaseStepCommandAndRemaining("^ scripted"),
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("^ publishSigned"),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    )
  )
}