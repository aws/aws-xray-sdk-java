plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":aws-xray-recorder-sdk-core"))

    compileOnly("org.apache.logging.log4j:log4j-api:2.17.0")

    testImplementation("org.apache.logging.log4j:log4j-api:2.17.0")
    testImplementation("org.mockito:mockito-core:3.12.4")

    // The packages below are necessary to make the
    //
    // `./gradlew aws-xray-recorder-sdk-log4j:dependencies --stacktrace`
    //
    // command work correctly
    compileOnly("com.google.errorprone:error_prone_annotations:2.5.1")

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.assertj:assertj-core:3.16.1")
    testImplementation("org.junit-pioneer:junit-pioneer:0.9.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.12.4")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.log4j")
    }
}

description = "AWS X-Ray Recorder SDK for Java – Log4J Trace ID Injection"
