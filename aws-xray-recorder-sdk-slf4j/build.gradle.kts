plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":aws-xray-recorder-sdk-core"))

    compileOnly("org.slf4j:slf4j-api:1.7.30")

    testImplementation("ch.qos.logback:logback-classic:1.3.0-alpha5")
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.mockito:mockito-core:3.12.4")

    // The packages below are necessary to make the
    //
    // `./gradlew aws-xray-recorder-sdk-slf4j:dependencies --stacktrace`
    //
    // command work correctly
    compileOnly("com.google.errorprone:error_prone_annotations:2.5.1")

    testImplementation("org.assertj:assertj-core:3.16.1")
    testImplementation("org.junit-pioneer:junit-pioneer:0.9.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.12.4")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.slf4j")
    }
}

description = "AWS X-Ray Recorder SDK for Java - SLF4J Trace ID Injection"
