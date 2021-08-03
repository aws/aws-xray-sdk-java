plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-aws-sdk-v2"))
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.aws-sdk-v2-instrumentor")
    }
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK V2 Instrumentor"
