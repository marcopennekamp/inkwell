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

lazy val quillVersion = "3.1.0"

lazy val commonSettings = Seq(
  organization := "app.wordpace",
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  scalaVersion := "2.12.8",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scalameta" %% "scalafmt-dynamic" % "2.0.0-RC6",
    "io.getquill" %% "quill-core" % quillVersion,
    "io.getquill" %% "quill-sql" % quillVersion % Test,
    "io.getquill" %% "quill-jdbc" % quillVersion % Test,
    "com.h2database" % "h2" % "1.4.196" % Test,
    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "org.slf4j" % "slf4j-log4j12" % "1.7.16" % Test,
  ),
  fork in Test := true,
  scalacOptions ++= Seq(
    "-feature",
    "-language:implicitConversions",
  ),
)

lazy val releaseSettings = ReleasePlugin.extraReleaseCommands ++ Seq(
  publishMavenStyle := true,
  publishArtifact := true,
  publishArtifact in Test := false,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) {
      Some("snapshots" at nexus + "content/repositories/snapshots")
    } else {
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    }
  },
  autoAPIMappings := true,
  homepage := Some(url("https://github.com/marcopennekamp/inkwell")),
  apiURL := Some(url("https://github.com/marcopennekamp/inkwell")),
  scmInfo := Some(ScmInfo(url("https://github.com/marcopennekamp/inkwell"),
    "scm:git:git@github.com:marcopennekamp/inkwell.git")),
  developers := List(
    Developer("marcopennekamp", "Marco Pennekamp", "marco@wordpace.app", url("https://marcopennekamp.com/"))
  ),
)
