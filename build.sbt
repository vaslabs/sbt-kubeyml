name := "sbt-kubeyml"

version := "0.1"

scalaVersion := "2.12.10"

lazy val scala212 = "2.12.10"

crossScalaVersions := Seq(scala212)

val circeVersion = "0.10.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-yaml" % circeVersion,
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.1" % "test"
)

lazy val pluginSettings = Seq(
  sbtPlugin := true,
  crossSbtVersions := Seq("1.2.8", "1.3.3")
)
pluginSettings

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

releaseSettings