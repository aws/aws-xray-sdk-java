plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-core"))

    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-all:1.10.19")
}

description = "AWS X-Ray Recorder SDK for Java - SQL Interceptor"
