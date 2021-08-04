plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-core"))

    compileOnly("org.apache.tomcat:tomcat-jdbc:8.0.36")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.sql_postgres")
    }
}
description = "AWS X-Ray Recorder SDK for Java - AWS SDK PostgreSQL Interceptor"
