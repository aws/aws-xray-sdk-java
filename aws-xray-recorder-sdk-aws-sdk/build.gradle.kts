plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":aws-xray-recorder-sdk-core"))

    implementation(project(":aws-xray-recorder-sdk-aws-sdk-core"))

    api("com.amazonaws:aws-java-sdk-core:1.11.1000")

    testImplementation("com.amazonaws:aws-java-sdk:1.11.1000")
    testImplementation("org.powermock:powermock-reflect:2.0.2")
    testImplementation("org.skyscreamer:jsonassert:1.3.0")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.aws_sdk")
    }
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK Handler"
