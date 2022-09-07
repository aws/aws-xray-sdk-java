plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-core"))

    compileOnly("com.amazonaws:aws-lambda-java-events:3.11.0")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.sqs")
    }
}
description = "AWS X-Ray Recorder SDK for Java - AWS SDK SQS Helper"
