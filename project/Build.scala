import sbt._

import Keys._
import AndroidKeys._

object General {
  val settings = Defaults.defaultSettings ++ Seq (
    name := "wordsteal",
    version := "0.1",
    versionCode := 0,
    scalaVersion := "2.10.0",
    platformName in Android := "android-4"
  )

  val proguardSettings = Seq (
    useProguard in Android := true
  )

  lazy val fullAndroidSettings =
    General.settings ++
    AndroidProject.androidSettings ++
    TypedResources.settings ++
    proguardSettings ++
    AndroidManifestGenerator.settings ++
    AndroidMarketPublish.settings ++ Seq (
      keyalias in Android := "shadaj",
      manifestPath in Android := Seq(file("AndroidManifest.xml")),
      mainResPath in Android := file("res"),
      mainAssetsPath in Android := file("assets"),
      managedScalaPath in Android := file("gen")
    )
}

object AndroidBuild extends Build {
  lazy val main = Project (
    "wordsteal",
    file("."),
    settings = General.fullAndroidSettings
  )
}