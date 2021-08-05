plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":aws-xray-recorder-sdk-core"))

    implementation(project(":aws-xray-recorder-sdk-aws-sdk-core"))

    api("software.amazon.awssdk:aws-core:2.15.20")

    testImplementation("org.skyscreamer:jsonassert:1.3.0")
    testImplementation("software.amazon.awssdk:dynamodb:2.15.20")
    testImplementation("software.amazon.awssdk:lambda:2.15.20")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.aws_sdk_v2")
    }
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK V2"
