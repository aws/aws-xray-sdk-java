plugins {
    `java-platform`
    `maven-publish`
}

description = "The AWS X-Ray Recorder SDK for Java - BOM"

dependencies {
    constraints {
        rootProject.subprojects {
            if (name.startsWith("aws-xray-recorder-sdk-") && !name.endsWith("-bom")) {
                api("${group}:${name}:${version}")
            }
        }
    }
}
