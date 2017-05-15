import ReleaseTransformations._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._
import sbt.Resolver.mavenLocal
import sbtrelease.ReleasePlugin

val quillVersion = "1.2.1"

lazy val `quill-trait` =
  (project in file("."))
    .settings(commonSettings: _*)
    .settings(tutSettings: _*)
    .dependsOn(
      `quill-trait-core-jvm`, `quill-trait-core-js`
    ).aggregate(
      `quill-trait-core-jvm`, `quill-trait-core-js`
    ).enablePlugins(TutPlugin)

lazy val `quill-trait-core` =
  crossProject.crossType(CrossType.Full)
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "io.getquill" %%% "quill-core" % quillVersion,
      "io.getquill" %% "quill-sql" % quillVersion % "test",
      "io.getquill" %% "quill-async-mysql" % quillVersion % "test",
      "io.getquill" %% "quill-async-postgres" % quillVersion % "test",
      "io.getquill" %% "quill-cassandra" % quillVersion % "test",
      "com.typesafe"               %  "config"        % "1.3.1",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "org.scala-lang"             %  "scala-reflect" % scalaVersion.value
    ))
    .jsSettings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-java-time" % "0.2.0"
      ),
      coverageExcludedPackages := ".*"
    ).jvmSettings(
      fork in Test := true
    )

lazy val `quill-trait-core-jvm` = `quill-trait-core`.jvm
lazy val `quill-trait-core-js` = `quill-trait-core`.js

lazy val tutSources = Seq(
  "README.md"
)

lazy val tutSettings = Seq(
  libraryDependencies ++= Seq(
    "io.getquill" %% "quill-sql" % quillVersion % "tut"
  ),
  scalacOptions in Tut := scalacOptions.value.filterNot(Set("-Ywarn-unused-import")),
  tutSourceDirectory := baseDirectory.value / "target" / "tut",
  tutNameFilter := tutSources.map(_.replaceAll("""\.""", """\.""")).mkString("(", "|", ")").r,
  sourceGenerators in Compile +=
    Def.task {
      tutSources.foreach { name =>
        val source = baseDirectory.value / name
        val file = baseDirectory.value / "target" / "tut" / name
        val str = IO.read(source).replace("```scala", "```tut")
        IO.write(file, str)
      }
      Seq()
    }.taskValue
)

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

def updateReadmeVersion(selectVersion: sbtrelease.Versions => String) =
  ReleaseStep(action = st => {

    val newVersion = selectVersion(st.get(ReleaseKeys.versions).get)

    import scala.io.Source
    import java.io.PrintWriter

    val pattern = """"com.github.nstojiljkovic" %% "quill-trait-.*" % "(.*)"""".r

    val fileName = "README.md"
    val content = Source.fromFile(fileName).getLines.mkString("\n")

    val newContent =
      pattern.replaceAllIn(content,
        m => m.matched.replaceAllLiterally(m.subgroups.head, newVersion))

    new PrintWriter(fileName) { write(newContent); close }

    val vcs = Project.extract(st).get(releaseVcs).get
    vcs.add(fileName).!

    st
  })

lazy val commonSettings = ReleasePlugin.extraReleaseCommands ++ Seq(
  organization := "com.github.nstojiljkovic",
  scalaVersion := "2.11.11",
  crossScalaVersions := Seq("2.11.11"),
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
  ),
  concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  scoverage.ScoverageKeys.coverageMinimum := 75,
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
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    setReleaseVersion,
    updateReadmeVersion(_._1),
    commitReleaseVersion,
    // updateWebsiteTag,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    updateReadmeVersion(_._2),
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
