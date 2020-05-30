plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":aws-xray-recorder-sdk-core"))

    implementation(project(":aws-xray-recorder-sdk-aws-sdk-core"))

    api("com.amazonaws:aws-java-sdk:1.11.398")

    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-all:1.10.19")
    testImplementation("org.skyscreamer:jsonassert:1.3.0")
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK Handler"
