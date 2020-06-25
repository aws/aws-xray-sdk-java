plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-core"))

    compileOnly("org.apache.tomcat:tomcat-jdbc:8.0.36")
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK PostgreSQL Interceptor"
