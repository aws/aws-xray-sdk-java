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
    testImplementation("com.amazonaws:aws-lambda-java-events:3.11.0")
    testImplementation("org.powermock:powermock-module-junit4:2.0.7")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray_lambda")
    }
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK Lambda Helper"
