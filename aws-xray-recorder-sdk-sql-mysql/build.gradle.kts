plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-core"))

    testImplementation("junit:junit:4.12")

    compileOnly("org.apache.tomcat:tomcat-jdbc:8.0.36")
}

description = "AWS X-Ray Recorder SDK for Java - AWS SDK MySQL Interceptor"
