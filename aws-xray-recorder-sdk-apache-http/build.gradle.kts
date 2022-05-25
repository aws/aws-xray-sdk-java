plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":aws-xray-recorder-sdk-core"))

    api("org.apache.httpcomponents:httpclient:4.5.13")

    testImplementation("com.github.tomakehurst:wiremock-jre8:2.33.2")
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.mockito:mockito-core:3.12.4")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.apache_http")
    }
}

description = "AWS X-Ray Recorder SDK for Java - Apache HTTP Client Proxy"
