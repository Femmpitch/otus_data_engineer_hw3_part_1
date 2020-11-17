name := "otus_scala_hw3"

version := "0.1"

scalaVersion := "2.11.12"

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-sql_2.11" % "2.4.7" % Provided,
  "org.json4s" % "json4s-jackson_2.11" % "3.6.10"
)