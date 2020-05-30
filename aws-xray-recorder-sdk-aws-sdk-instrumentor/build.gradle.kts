plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnly(project(":aws-xray-recorder-sdk-aws-sdk"))
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK Instrumentor"
