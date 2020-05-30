plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":aws-xray-recorder-sdk-core"))

    api("org.apache.httpcomponents:httpclient:4.5.2")

    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-all:1.10.19")
}

description = "AWS X-Ray Recorder SDK for Java - Apache HTTP Client Proxy"
