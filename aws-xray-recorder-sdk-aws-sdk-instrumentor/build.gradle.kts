plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-aws-sdk"))

    // The packages below are necessary to make the
    //
    // `./gradlew aws-xray-recorder-sdk-aws-sdk:dependencies --stacktrace`
    //
    // command work correctly
    compileOnly("com.google.errorprone:error_prone_annotations:2.5.1")

    testImplementation("org.junit-pioneer:junit-pioneer:0.9.0")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.aws_sdk_instrumentor")
    }
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK Instrumentor"
