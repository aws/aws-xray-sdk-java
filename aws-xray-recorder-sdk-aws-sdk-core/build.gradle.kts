plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:2.11.0")

    testImplementation("com.fasterxml.jackson.core:jackson-databind")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.aws_sdk_core")
    }
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK Core"
