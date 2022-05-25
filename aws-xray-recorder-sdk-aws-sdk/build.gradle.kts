plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    // TODO: (enowell) If we change this to `implementation` the tests still
    // pass. `implementation` is preferred, but it means this won't be
    // transitively available to downstream packages like `api` does. We should
    // consider making the change.
    //
    // See: https://stackoverflow.com/a/47365147
    api(project(":aws-xray-recorder-sdk-core"))

    implementation(project(":aws-xray-recorder-sdk-aws-sdk-core"))

    // TODO: (enowell) If we delete this, the package tests still pass. However,
    // as `api` it is transitively available to downstream packages. We should
    // consider deleting this.
    //
    // See: https://stackoverflow.com/a/47365147
    api("com.amazonaws:aws-java-sdk-core:1.12.227")

    testImplementation("com.amazonaws:aws-java-sdk:1.12.227")
    testImplementation("org.powermock:powermock-reflect:2.0.9")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.mockito:mockito-core:3.12.4")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.aws_sdk")
    }
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK Handler"
