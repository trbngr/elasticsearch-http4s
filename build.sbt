import Dependencies._
import com.amazonaws.services.s3.model.Region

lazy val buildSettings = Seq(
  version := "2.4.0",
  organization := "com.linktargeting.elasticsearch",
  name := "Elasticsearch Http Client for Scala",
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.11.8", scalaVersion.value),
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-Xlint:-infer-any", "-Xfatal-warnings", "-language:postfixOps", "-language:implicitConversions"),
  testOptions in Test += Tests.Argument("-oD"),

  //Let CTRL+C kill the current task and not the whole SBT session.
  cancelable in Global := true
)

lazy val publishSettings = Seq(
  s3region := Region.US_West_2,
  s3overwrite := true,
  publishArtifact in Test := false,
  pomIncludeRepository := (_ ⇒ true),
  publishMavenStyle := true,
  publishTo := {
    val folder = if (isSnapshot.value) "snapshot" else "release"
    Some(s3resolver.value(s"$folder LinkTargeting", s3(s"repo.linktargeting.io/$folder")) withMavenPatterns)
  }
)

lazy val root = project.in(file("."))
  .settings(buildSettings: _*)
  .settings(
    moduleName := "elasticsearch-client",
    publishArtifact := false,
    publish := {}
  )
  .aggregate(core, aws, circe, test, akka)

lazy val core = project.in(file("elasticsearch-core"))
  .settings(buildSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    moduleName := "elasticsearch-core",
    libraryDependencies ++= Seq()
  )

lazy val test = project.in(file("elasticsearch-test"))
  .settings(buildSettings: _*)
  .settings(
    moduleName := "elasticsearch-test",
    libraryDependencies ++= Seq(scalaTest),
    libraryDependencies ++= Seq(elasticsearch),
    publish := {}
  )
  .dependsOn(core)

lazy val akka = project.in(file("modules/elasticsearch-akka"))
  .settings(buildSettings: _*)
  .settings(
    moduleName := "elasticsearch-akka",
    libraryDependencies ++= Seq(akkaHttp, akkaStream, akkaSlf4j, akkaTestKit)
  )
  .dependsOn(core, test % "test", circe % "test")
  .settings(publishSettings: _*)

lazy val aws = project.in(file("modules/elasticsearch-aws"))
  .settings(buildSettings: _*)
  .settings(
    moduleName := "elasticsearch-aws",
    libraryDependencies ++= Seq(awsSdkCore),
    libraryDependencies ++= Seq(scalaTest % "test")
  )
  .dependsOn(core)
  .settings(publishSettings: _*)

lazy val circe = project.in(file("modules/elasticsearch-circe"))
  .settings(buildSettings: _*)
  .settings(
    moduleName := "elasticsearch-circe",
    libraryDependencies ++= Dependencies.circe,
    libraryDependencies ++= Seq(scalaTest % "test")
  )
  .dependsOn(core, test % "test")
  .settings(publishSettings: _*)