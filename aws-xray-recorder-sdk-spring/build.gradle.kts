plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":aws-xray-recorder-sdk-core"))

    api("org.aspectj:aspectjrt:1.8.11")

    // TODO(anuraaga): Remove most of these? Seems only Configurable annotation is used
    implementation("org.springframework:spring-context-support:5.3.18")
    implementation("org.springframework:spring-context:5.3.18")
    implementation("org.springframework:spring-aspects:5.3.18")

    compileOnly("org.springframework.data:spring-data-commons:2.6.3")

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.mockito:mockito-core:3.12.4")

    // The packages below are necessary to make the
    //
    // `./gradlew aws-xray-recorder-sdk-spring:dependencies --stacktrace`
    //
    // command work correctly
    compileOnly("com.google.errorprone:error_prone_annotations:2.5.1")

    testImplementation("org.assertj:assertj-core:3.16.1")
    testImplementation("org.junit-pioneer:junit-pioneer:0.9.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.12.4")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.spring")
    }
}

description = "AWS X-Ray Recorder SDK for Java - Spring Framework Interceptors"
