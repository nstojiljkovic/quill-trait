import ReleaseTransformations._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt.Resolver.mavenLocal

import scalariform.formatter.preferences._
import sbtrelease.ReleasePlugin

val quillVersion = "1.1.1"

val scalaV = "2.11.8"
//scalaOrganization in ThisBuild := "org.typelevel"
scalaVersion := scalaV
val pprintVersion = "0.4.4"

lazy val `quill-trait` =
  crossProject.crossType(superPure)
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "pprint" % pprintVersion,
      "io.getquill" %%% "quill-core" % quillVersion,
      "io.getquill" %% "quill-async-mysql" % quillVersion % "test",
      "io.getquill" %% "quill-async-postgres" % quillVersion % "test",
      "io.getquill" %% "quill-cassandra" % quillVersion % "test",
      "com.typesafe"               %  "config"        % "1.3.1",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "org.scala-lang"             %  "scala-reflect" % scalaVersion.value
    ))
    .jsSettings(
      libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "0.2.0",
      coverageExcludedPackages := ".*"
    ).jvmSettings(
      fork in Test := true
    )

lazy val `quill-trait-jvm` = `quill-trait`.jvm
lazy val `quill-trait-js` = `quill-trait`.js

lazy val superPure = new org.scalajs.sbtplugin.cross.CrossType {
  def projectDir(crossBase: File, projectType: String): File =
    projectType match {
      case "jvm" => crossBase
      case "js"  => crossBase / s".$projectType"
    }

  def sharedSrcDir(projectBase: File, conf: String): Option[File] =
    Some(projectBase.getParentFile / "src" / conf / "scala")
}

lazy val mimaSettings = MimaPlugin.mimaDefaultSettings ++ Seq(
  mimaPreviousArtifacts := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor <= 11 =>
        Set(organization.value % s"${name.value}_${scalaBinaryVersion.value}" % "0.5.0")
      case _ =>
        Set()
    }
  }
)

lazy val commonSettings = ReleasePlugin.extraReleaseCommands ++ Seq(
  organization := "com.nikolastojiljkovic",
  scalaVersion := scalaV,
  libraryDependencies ++= Seq(
    "org.scalamacros" %% "resetallattrs"  % "1.0.0",
    "org.scalatest"   %%% "scalatest"     % "3.0.1"     % Test,
    "ch.qos.logback"  % "logback-classic" % "1.2.3"     % Test,
    "com.google.code.findbugs" % "jsr305" % "3.0.2"     % Provided // just to avoid warnings during compilation
  ),
  EclipseKeys.createSrc := EclipseCreateSrc.Default,
  unmanagedClasspath in Test ++= Seq(
    baseDirectory.value / "src" / "test" / "resources"
  ),
  EclipseKeys.eclipseOutput := Some("bin"),
  scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture",
    "-Ywarn-unused-import"
  ),
  resolvers ++= Seq(
    mavenLocal,
    "Restlet Repository" at "http://maven.restlet.org/",
    "JBoss Repository" at "https://repository.jboss.org/nexus/content/repositories/",
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "Scala-Tools Snapshots" at "http://scala-tools.org/repo-snapshots/"
//    ,
//    "Essential Dots Artifactory" at "http://artifactory.essentialdots.com/artifactory/sbt-local/"
  ),
  concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  scoverage.ScoverageKeys.coverageMinimum := 96,
  scoverage.ScoverageKeys.coverageFailOnMinimum := false,
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignParameters, true)
    .setPreference(CompactStringConcatenation, false)
    .setPreference(IndentPackageBlocks, true)
    .setPreference(FormatXml, true)
    .setPreference(PreserveSpaceBeforeArguments, false)
    .setPreference(DoubleIndentClassDeclaration, false)
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
    .setPreference(SpaceBeforeColon, false)
    .setPreference(SpaceInsideBrackets, false)
    .setPreference(SpaceInsideParentheses, false)
    .setPreference(DanglingCloseParenthesis, Force)
    .setPreference(IndentSpaces, 2)
    .setPreference(IndentLocalDefs, false)
    .setPreference(SpacesWithinPatternBinders, true)
    .setPreference(SpacesAroundMultiImports, true),
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
//  pgpSecretRing := file("local.secring.gpg"),
//  pgpPublicRing := file("local.pubring.gpg"),
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    // updateReadmeVersion(_._1),
    commitReleaseVersion,
    //updateWebsiteTag,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _)),
    setNextVersion,
    // updateReadmeVersion(_._2),
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  ),
  pomExtra := (
    <url>https://github.com/nstojiljkovic/quill-trait</url>
    <licenses>
      <license>
        <name>Apache License 2.0</name>
        <url>https://raw.githubusercontent.com/nstojiljkovic/quill-trait/master/LICENSE</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:nstojiljkovic/quill-trait.git</url>
      <connection>scm:git@github.com:nstojiljkovic/quill-trait.git</connection>
    </scm>
    <developers>
      <developer>
        <id>nstojiljkovic</id>
        <name>Nikola Stojiljković</name>
        <url>https://github.com/nstojiljkovic</url>
      </developer>
    </developers>)
)
