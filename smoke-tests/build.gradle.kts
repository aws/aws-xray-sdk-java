plugins {
    `java-library`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-core"))

    testImplementation("org.mockito:mockito-core:3.12.4")
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
