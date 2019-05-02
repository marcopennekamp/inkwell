import java.io.{File => JFile}
import java.nio.file.{Files, Path => NioPath, Paths}

import scala.collection.JavaConverters._
import scala.util.Try

// sbt-pgp's publishLocalSigned task requires all projects  to define the publishTo setting, despite publishing
// local, UNLESS the project is skipped. Since a root project is defined implicitly if an explicit definition is
// absent, we have to manually skip publishing the root project.
lazy val root = (project in file("."))
  .aggregate(inkwell, `generate-test-schema`, `test-generated-code`)
  .settings(skip in publish := true)

lazy val inkwell =
  (project in file("inkwell"))
    .settings(commonSettings ++ releaseSettings)
    .settings(
      name := "inkwell",
      version := "0.1.0",
    )

val codeGen = taskKey[Seq[File]]("Run code generation for test schema tests")

// Provides the generators that generate test schemas.
lazy val `generate-test-schema` =
  (project in file("generate-test-schema"))
    .settings(commonSettings ++ schemaTestSettings)
    .settings(skip in publish := true)
    .dependsOn(inkwell)

// Tests the code generated from test schemas.
lazy val `test-generated-code` =
  (project in file("test-generated-code"))
    .settings(commonSettings ++ schemaTestSettings)
    .settings(
      skip in publish := true,
      (sourceGenerators in Compile) += (codeGen in Compile),
      (codeGen in Compile) := {
        val sourcePath = Paths.get(sourceManaged.value.getPath, "main")
        val classPath = (fullClasspath in Compile in `generate-test-schema`).value.map(_.data)
        (runner in Compile).value.run(
          "app.wordpace.inkwell.test.GeneratorRunner",
          classPath, Seq(sourcePath.toString), streams.value.log
        )

        // Discover all generated files under src_managed/main in integration-tests.
        var stream: Option[java.util.stream.Stream[NioPath]] = None
        val files: Try[Seq[JFile]] = Try {
          stream = Some(Files.walk(sourcePath))
          stream.get.iterator.asScala.toVector.map(_.toFile).filter(_.isFile)
        }
        stream.foreach(s => s.close())
        files.get
      },
    )
    .dependsOn(`generate-test-schema`)

lazy val quillVersion = "3.1.0"

lazy val commonSettings = Seq(
  organization := "app.wordpace",
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  scalaVersion := "2.12.8",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scalameta" %% "scalafmt-dynamic" % "2.0.0-RC6",
    "io.getquill" %% "quill-core" % quillVersion,
    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "org.slf4j" % "slf4j-log4j12" % "1.7.16" % Test,
  ),
  fork in Test := true,
  scalacOptions ++= Seq(
    "-feature",
    "-language:implicitConversions",
  ),
)

lazy val schemaTestSettings = Seq(
  libraryDependencies ++= Seq(
    "com.h2database" % "h2" % "1.4.196",
    "io.getquill" %% "quill-sql" % quillVersion % Test,
    "io.getquill" %% "quill-jdbc" % quillVersion % Test,
  ),
)

lazy val releaseSettings = Seq(
  publishMavenStyle := true,
  publishArtifact := true,
  publishArtifact in Test := false,
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
