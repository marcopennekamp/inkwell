import java.io.FileOutputStream

import ReleaseTransformations._
import sbtrelease.ReleasePlugin
import java.io.{File => JFile}

import scala.util.{Failure, Success, Try}

lazy val `generator` =
  (project in file("generator"))
    .settings(commonSettings ++ releaseSettings)
    .settings(
      name := "inkwell",
      fork in Test := true
    )

val codeGen = taskKey[Seq[File]]("Run code generation for integration tests")

lazy val `integration-tests` =
  (project in file("integration-tests"))
    .settings(commonSettings)
    .settings(
      publishArtifact := false,
      fork in Test := true,
      (sourceGenerators in Compile) += (codeGen in Compile),
      (codeGen in Compile) := {
        //  import scala.concurrent.JavaConversions._
        def recrusiveList(file:JFile): List[JFile] = {
          if (file.isDirectory)
            Option(file.listFiles()).map(_.flatMap(child=> recrusiveList(child)).toList).toList.flatten
          else
            List(file)
        }

    val r = (runner in Compile).value
    val s = streams.value.log
    val sourcePath = sourceManaged.value
    val classPath = (fullClasspath in Test in `generator`).value.map(_.data)

    val fileDir = new File(sourcePath, "main").getAbsoluteFile
    r.run(
      "app.wordpace.inkwell.integration.GeneratorRunner",
      classPath, Seq(fileDir.getAbsolutePath), s
    )

        recrusiveList(fileDir)
      }
    )
    .dependsOn(generator % "compile->test")

lazy val quillVersion = "2.3.1"

lazy val commonSettings = Seq(
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  organization := "com.github.choppythelumberjack",
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.11.12","2.12.4"),
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
)

lazy val releaseSettings = ReleasePlugin.extraReleaseCommands ++ Seq(
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  releaseCrossBuild := true,
  organization := "com.github.choppythelumberjack",
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.11.12","2.12.4"),
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
