plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-aws-sdk-v2"))
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.aws_sdk_v2_instrumentor")
    }
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK V2 Instrumentor"
