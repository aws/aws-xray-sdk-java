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

    // The packages below are necessary to make the
    //
    // `./gradlew aws-xray-recorder-sdk-apache-http:dependencies --stacktrace`
    //
    // command work correctly
    compileOnly("com.google.errorprone:error_prone_annotations:2.5.1")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.apache_http")
    }
}

description = "AWS X-Ray Recorder SDK for Java - Apache HTTP Client Proxy"
