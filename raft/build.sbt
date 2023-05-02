val scala3Version = "3.2.2"
lazy val akkaVersion = "2.8.0"
lazy val root = project
  .in(file("."))
  .settings(
    name := "akkademo1",
    version := "0.1.0-SNAPSHOT",
    fork := true,
    connectInput in run := true, // This is required for console input to work!

    // This is required as a workaround for https://github.com/lampepfl/dotty/issues/14846
    compileOrder := CompileOrder.JavaThenScala,
    scalaVersion := scala3Version,
    javacOptions ++= Seq("-source", "17", "-target", "17", "--enable-preview"),
    javaOptions ++= Seq("--enable-preview"),

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.4.6",
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.12" % Test,
    )
  )
