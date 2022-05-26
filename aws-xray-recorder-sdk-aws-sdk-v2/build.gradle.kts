plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":aws-xray-recorder-sdk-core"))

    implementation(project(":aws-xray-recorder-sdk-aws-sdk-core"))

    api("software.amazon.awssdk:aws-core:2.15.20")

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.mockito:mockito-junit-jupiter:3.12.4")
    testImplementation("org.skyscreamer:jsonassert:1.3.0")
    testImplementation("software.amazon.awssdk:dynamodb:2.15.20")
    testImplementation("software.amazon.awssdk:lambda:2.15.20")

    // The packages below are necessary to make the
    //
    // `./gradlew aws-xray-recorder-sdk-aws-sdk-v2:dependencies --stacktrace`
    //
    // command work correctly
    compileOnly("com.google.errorprone:error_prone_annotations:2.5.1")

    testImplementation("org.assertj:assertj-core:3.16.1")
    testImplementation("org.junit-pioneer:junit-pioneer:0.9.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.12.4")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.aws_sdk_v2")
    }
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK V2"
