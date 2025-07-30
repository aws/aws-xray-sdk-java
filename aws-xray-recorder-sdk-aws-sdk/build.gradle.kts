plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":aws-xray-recorder-sdk-core"))

    implementation(project(":aws-xray-recorder-sdk-aws-sdk-core"))

    // TODO: Try and find a way to declare aws-java-sdk dependencies
    //  via a bom in the dependencyManagement project and make it available
    //  for resolution not only in the SDK projects but also in projects
    //  like benchmark.
    //  See PR for more details: https://github.com/aws/aws-xray-sdk-java/pull/336
    api("com.amazonaws:aws-java-sdk-core:1.12.708")

    testImplementation("com.amazonaws:aws-java-sdk-lambda:1.12.228")
    testImplementation("com.amazonaws:aws-java-sdk-s3:1.12.228")
    testImplementation("com.amazonaws:aws-java-sdk-sns:1.12.228")
    testImplementation("com.amazonaws:aws-java-sdk-xray:1.12.228")
    testImplementation("org.skyscreamer:jsonassert:1.3.0")
    testImplementation("org.powermock:powermock-module-junit4:2.0.7")
    testImplementation("org.powermock:powermock-api-mockito2:2.0.7")
    testImplementation("com.github.stefanbirkner:system-rules:1.16.0")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.aws_sdk")
    }
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK Handler"
