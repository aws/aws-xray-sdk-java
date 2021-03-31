plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-core"))
    implementation("com.blogspot.mydailyjava:weak-lock-free:0.18")
}

description = "AWS X-Ray Recorder SDK for Java - SQL Interceptor"
