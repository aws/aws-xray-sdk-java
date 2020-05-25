plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnly(project(":aws-xray-recorder-sdk-aws-sdk-v2"))
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK V2 Instrumentor"
