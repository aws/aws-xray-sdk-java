plugins {
    `java-library`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-core"))

    testImplementation("org.mockito:mockito-core:3.12.4")
    testImplementation("org.mockito:mockito-junit-jupiter:3.12.4")

    // The packages below are necessary to make the
    //
    // `./gradlew smoke-tests:dependencies --stacktrace`
    //
    // command work correctly
    compileOnly("com.google.errorprone:error_prone_annotations:2.5.1")

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.assertj:assertj-core:3.16.1")
    testImplementation("org.junit-pioneer:junit-pioneer:0.9.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.12.4")
}

if (rootProject.findProperty("testDistributionChannel") == "true") {
    configurations.all {
        resolutionStrategy {
            dependencySubstitution {
                substitute(project(":aws-xray-recorder-sdk-core")).with(module("com.amazonaws:aws-xray-recorder-sdk-core:+"))
            }
        }
    }
}
