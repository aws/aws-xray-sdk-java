plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-aws-sdk-v2"))

    // The packages below are necessary to make the
    //
    // `./gradlew aws-xray-recorder-sdk-aws-sdk-v2-instrumentor:dependencies --stacktrace`
    //
    // command work correctly
    compileOnly("com.google.errorprone:error_prone_annotations:2.5.1")

    testImplementation("org.junit-pioneer:junit-pioneer:0.9.0")
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.assertj:assertj-core:3.16.1")
    testImplementation("org.mockito:mockito-core:3.12.4")
    testImplementation("org.mockito:mockito-junit-jupiter:3.12.4")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.aws_sdk_v2_instrumentor")
    }
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK V2 Instrumentor"
