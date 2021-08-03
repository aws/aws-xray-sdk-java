plugins {
    id("me.champeau.gradle.jmh") version "0.5.0"
}

val JMH_VERSION = "1.23"

sourceSets {
    named("jmh") {
        java {
            srcDir("tst/main/java")
        }
    }
}

dependencies {
    jmh(project(":aws-xray-recorder-sdk-core"))

    jmh("org.openjdk.jmh:jmh-generator-annprocess:${JMH_VERSION}")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:${JMH_VERSION}")

    add("jmhCompileClasspath", platform(project(":dependencyManagement")))
    add("jmhRuntimeClasspath", platform(project(":dependencyManagement")))
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.benchmark")
    }
}

jmh {
    fork = 1
    // Required when also including annotation processor.
    duplicateClassesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true
}
