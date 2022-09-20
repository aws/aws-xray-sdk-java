plugins {
    `java-library`
    `maven-publish`
}


buildscript {
    dependencies {
       classpath("com.amazonaws:aws-lambda-java-events:3.11.0")
    }
}

dependencies {
    compileOnly("com.amazonaws:aws-lambda-java-events:3.11.0")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray_sqs")
    }
}
description = "AWS X-Ray Recorder SDK for Java - AWS SDK SQS Helper"
