plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-aws-sdk"))
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.aws_sdk_instrumentor")
    }
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK Instrumentor"
