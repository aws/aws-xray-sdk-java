plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-core"))

    testImplementation("junit:junit:4.13.1")
    compileOnly("org.apache.tomcat:tomcat-jdbc:8.0.36")

    // The packages below are necessary to make the
    //
    // `./gradlew aws-xray-recorder-sdk-sql-postgres:dependencies --stacktrace`
    //
    // command work correctly
    compileOnly("com.google.errorprone:error_prone_annotations:2.5.1")

    testImplementation("org.assertj:assertj-core:3.16.1")
    testImplementation("org.junit-pioneer:junit-pioneer:0.9.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.12.4")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.amazonaws.xray.sql_postgres")
    }
}
description = "AWS X-Ray Recorder SDK for Java - AWS SDK PostgreSQL Interceptor"
