import java.io.{File => JFile}
import java.nio.file.{Files, Path, Paths}

import sbtrelease.ReleasePlugin

import scala.collection.JavaConverters._
import scala.util.Try

lazy val `generator` =
  (project in file("generator"))
    .settings(commonSettings ++ releaseSettings)
    .settings(
      name := "inkwell",
      version := "0.1.0-SNAPSHOT",
    )

val codeGen = taskKey[Seq[File]]("Run code generation for integration tests")

lazy val `integration-tests` =
  (project in file("integration-tests"))
    .settings(commonSettings)
    .settings(
      publishArtifact := false,
      (sourceGenerators in Compile) += (codeGen in Compile),
      (codeGen in Compile) := {
        val sourcePath = Paths.get(sourceManaged.value.getPath, "main")
        val classPath = (fullClasspath in Test in `generator`).value.map(_.data)
        (runner in Compile).value.run(
          "app.wordpace.inkwell.integration.GeneratorRunner",
          classPath, Seq(sourcePath.toString), streams.value.log
        )

        // Discover all generated files under src_managed/main in integration-tests.
        var stream: Option[java.util.stream.Stream[Path]] = None
        val files: Try[Seq[JFile]] = Try {
          stream = Some(Files.walk(sourcePath))
          stream.get.iterator.asScala.toVector.map(_.toFile).filter(_.isFile)
        }
        stream.foreach(s => s.close())
        files.get
      },
    )
    .dependsOn(generator % "compile->test")

lazy val quillVersion = "2.3.1"

lazy val commonSettings = Seq(
  organization := "app.wordpace",
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  scalaVersion := "2.12.8",
  libraryDependencies ++= Seq(
    "com.github.choppythelumberjack" %% "tryclose" % "1.0.0",
    "commons-lang" % "commons-lang" % "2.6",
    "io.getquill" %% "quill-core" % quillVersion,
    "io.getquill" %% "quill-sql" % quillVersion,
    "io.getquill" %% "quill-jdbc" % quillVersion,
    "org.scalameta" %% "scalafmt-dynamic" % "2.0.0-RC6",
    "com.h2database" % "h2" % "1.4.196" % Test,
    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "org.slf4j" % "slf4j-log4j12" % "1.7.16" % Test,
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % Test,
    "com.github.scopt" %% "scopt" % "4.0.0-RC2"
  )
  fork in Test := true,
)

lazy val releaseSettings = ReleasePlugin.extraReleaseCommands ++ Seq(
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  releaseProcess := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) =>
        Seq[ReleaseStep](
          checkSnapshotDependencies,
          inquireVersions,
          runClean,
          setReleaseVersion,
          commitReleaseVersion,
          tagRelease,
          publishArtifacts,
          setNextVersion,
          commitNextVersion,
          releaseStepCommand("sonatypeReleaseAll"),
          pushChanges
        )
      case Some((2, 12)) =>
        Seq[ReleaseStep](
          checkSnapshotDependencies,
          inquireVersions,
          runClean,
          setReleaseVersion,
          publishArtifacts,
          releaseStepCommand("sonatypeReleaseAll")
        )
      case _ => Seq[ReleaseStep]()
    }
  },
  pomExtra := (
      <url>https://github.com/choppythelumberjack</url>
      <scm>
        <connection>scm:git:git@github.com:choppythelumberjack/trivial-codegen.git</connection>
        <developerConnection>scm:git:git@github.com:choppythelumberjack/trivial-codegen.git</developerConnection>
        <url>https://github.com/choppythelumberjack/trivial-codegen</url>
      </scm>
      <developers>
        <developer>
          <id>choppythelumberjack</id>
          <name>Choppy The Lumberjack</name>
          <url>https://github.com/choppythelumberjack</url>
        </developer>
      </developers>)
)
