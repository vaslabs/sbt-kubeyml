import microsites.ExtraMdFileConfig
import xerial.sbt.Sonatype.GitHubHosting

name := "sbt-kubeyml"

scalaVersion := "2.12.20"

lazy val scala212 = "2.12.20"

lazy val `kubeyml` = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin, SbtPlugin)
  .settings(pluginSettings)
  .settings(
    name := "sbt-kubeyml"
  )
  .settings(publishSettings)
  .settings(releaseSettings)
  .settings(pluginSettings)
  .settings(compilerSettings)

lazy val site = (project in file("site"))
  .enablePlugins(MicrositesPlugin, MdocPlugin)
  .settings(docSettings)
  .settings(noPublishSettings)
  .dependsOn(`kubeyml`)

crossScalaVersions := Seq(scala212)

val circeVersion = "0.14.10"
credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credential")

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-yaml" % "0.15.2",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % Test,
  "org.scalacheck" %% "scalacheck" % "1.18.1" % Test,
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.15" % "1.3.0" % Test
)
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.0")

lazy val pluginSettings = Seq(
  sbtPlugin := true
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
  scmInfo := Some(ScmInfo(url("https://github.com/vaslabs/sbt-kubeyml"), "scm:git@github.com:vaslabs/sbt-kubeyml.git")),
  developers := List(
    Developer(
      id = "vaslabs",
      name = "Vasilis Nicolaou",
      email = "vaslabsco@gmail.com",
      url = url("http://vaslabs.org")
    )
  ),
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
  publishMavenStyle := true,
  licenses := List("MIT" -> new URL("https://opensource.org/licenses/MIT")),
  homepage := Some(url("https://git.vaslabs.org/vaslabs/sbt-kubeyml")),
  startYear := Some(2019)
)

lazy val releaseSettings = {
  import ReleaseTransformations._

  Seq(
    releaseCrossBuild := false,
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

lazy val compilerSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-language:postfixOps", // Allow postfix operator notation, such as `1 to 10 toList'
    "-language:implicitConversions",
    "-language:higherKinds",
    "-Ypartial-unification",
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen", // Warn when numerics are widened.
    "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals", // Warn if a local definition is unused.
    "-Ywarn-unused:params", // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates", // Warn if a private member is unused.
    "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
    "-Ywarn-unused:imports",
    "-Xfatal-warnings"
  )
)

lazy val docSettings = Seq(
  micrositeName := "sbt-kubeyml",
  micrositeDescription := "Autogenerate kubernetes manifests for your scala application",
  micrositeAuthor := "Vasilis Nicolaou",
  micrositeTwitter := "@vaslabs",
  micrositeTwitterCreator := "@vaslabs",
  micrositeGithubOwner := "vaslabs",
  micrositeGithubRepo := "sbt-kubeyml",
  micrositeUrl := "https://sbt-kubeyml.vaslabs.org",
  micrositePushSiteWith := GHPagesPlugin,
  micrositeGitterChannel := false,
  micrositeExtraMdFiles := Map(
    file("README.md") -> ExtraMdFileConfig(
      "index.md",
      "home",
      Map("section" -> "home", "position" -> "0", "permalink" -> "/")
    )
  ),
  ghpagesCleanSite / excludeFilter :=
    new FileFilter {
      def accept(f: File) = (ghpagesRepository.value / "CNAME").getCanonicalPath == f.getCanonicalPath
    } || "versions.html"
)
lazy val noPublishSettings =
  Seq(
    publish / skip := true,
    publish := (()),
    publishLocal := (()),
    publishArtifact := false,
    publishTo := None
  )
