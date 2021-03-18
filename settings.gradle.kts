pluginManagement {
    plugins {
        id("com.github.hierynomus.license") version "0.15.0"
        id("nebula.release") version "15.1.0"
        id("io.freefair.aggregate-javadoc-jar") version "5.3.0"
        id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
        id("net.ltgt.errorprone") version "1.2.1"
        id("org.ajoberstar.grgit") version "4.0.2"
        id("org.checkerframework") version "0.5.4"
    }
}

include(":aws-xray-recorder-sdk-apache-http")
include(":aws-xray-recorder-sdk-aws-sdk")
include(":aws-xray-recorder-sdk-aws-sdk-instrumentor")
include(":aws-xray-recorder-sdk-benchmark")
include(":aws-xray-recorder-sdk-bom")
include(":aws-xray-recorder-sdk-core")
include(":aws-xray-recorder-sdk-sql")
include(":aws-xray-recorder-sdk-sql-mysql")
include(":aws-xray-recorder-sdk-sql-postgres")
include(":aws-xray-recorder-sdk-spring")
include(":aws-xray-recorder-sdk-aws-sdk-core")
include(":aws-xray-recorder-sdk-aws-sdk-v2")
include(":aws-xray-recorder-sdk-aws-sdk-v2-instrumentor")
include(":aws-xray-recorder-sdk-slf4j")
include(":aws-xray-recorder-sdk-log4j")
include(":aws-xray-recorder-sdk-metrics")

// Internal project for applying dependency management.
include(":dependencyManagement")
// Project for generating aggregated Jacoco code coverage report.
include(":jacoco")
// Internal project for smoke tests
include(":smoke-tests")
