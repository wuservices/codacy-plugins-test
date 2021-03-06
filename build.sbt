name := """codacy-plugins-test"""

val scalaBinaryVersionNumber = "2.12"
scalaVersion := s"$scalaBinaryVersionNumber.6"

resolvers := Seq("Sonatype OSS Snapshots".at("https://oss.sonatype.org/content/repositories/releases"),
                 "Codacy Public Mvn bucket".at("https://s3-eu-west-1.amazonaws.com/public.mvn.codacy.com"),
                 "Typesafe Repo".at("http://repo.typesafe.com/typesafe/releases/")) ++ resolvers.value

scalacOptions ++= Common.compilerFlags

libraryDependencies ++= Seq(Dependencies.scalaTest, Dependencies.analysisCore, Dependencies.commonsIO)
