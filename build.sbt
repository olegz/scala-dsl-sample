name := "spring-dsl-sample"

version := "1.0"

organization := "spring.integration.scala.dsl"

scalaVersion := "2.9.2"

resolvers += "Spring Milestone Repository" at "http://repo.springsource.org/libs-milestone"

libraryDependencies += "org.springframework.integration" % "spring-integration-dsl-scala-core" % "1.0.0.M2"

libraryDependencies += "org.springframework.integration" % "spring-integration-dsl-scala-jms" % "1.0.0.M2"

libraryDependencies += "org.springframework.integration" % "spring-integration-dsl-scala-http" % "1.0.0.M2"

libraryDependencies += "com.novocode" % "junit-interface" % "0.7" % "test->default"

libraryDependencies += "log4j" % "log4j" % "1.2.16" % "test->default"

libraryDependencies += "junit" % "junit" % "4.8.1" % "test->default"

libraryDependencies += "org.apache.activemq" % "activemq-core" % "5.3.0" % "test->default"
