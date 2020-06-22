plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-core"))
}

description = "AWS X-Ray Recorder SDK for Java - SQL Interceptor"
