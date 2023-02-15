plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api("com.amazonaws:aws-java-sdk-xray:1.12.228")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("javax.servlet:javax.servlet-api:3.1.0")
    compileOnly("jakarta.servlet:jakarta.servlet-api:4.0.2")

    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation("com.github.stefanbirkner:system-rules:1.16.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8")
    testImplementation("org.openjdk.jmh:jmh-core:1.19")
    testImplementation("org.powermock:powermock-module-junit4:2.0.7")
    testImplementation("org.powermock:powermock-api-mockito2:2.0.7")
    testImplementation("org.skyscreamer:jsonassert:1.3.0")
    testImplementation("jakarta.servlet:jakarta.servlet-api:4.0.2")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.sdk_core")
    }
}

description = "AWS X-Ray Recorder SDK for Java - Core"
