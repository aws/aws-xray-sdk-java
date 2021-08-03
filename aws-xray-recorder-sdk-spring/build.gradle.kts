plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":aws-xray-recorder-sdk-core"))

    api("org.aspectj:aspectjrt:1.8.11")

    // TODO(anuraaga): Remove most of these? Seems only Configurable annotation is used
    implementation("org.springframework:spring-context-support:4.3.12.RELEASE")
    implementation("org.springframework:spring-context:4.3.12.RELEASE")
    implementation("org.springframework:spring-aspects:4.3.12.RELEASE")

    compileOnly("org.springframework.data:spring-data-commons:2.0.0.RELEASE")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.spring")
    }
}

description = "AWS X-Ray Recorder SDK for Java - Spring Framework Interceptors"
