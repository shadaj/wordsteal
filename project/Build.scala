import sbt._

import Keys._

import Defaults._

import sbtandroid.AndroidPlugin._

object AndroidBuild extends Build {
  val globalSettings = Seq (
    name := "wordsteal",
    version := "0.1",
    versionCode := 0,
    scalaVersion := "2.10.2",
    platformName := "android-4",
    useProguard := true,
    keyalias := "shadaj",
    mainResPath := file("res"),
    mainAssetsPath := file("assets"),
    managedScalaPath := file("gen")
  )

  lazy val main = AndroidProject (
    "wordsteal",
    file("."),
    settings = globalSettings
  )
}