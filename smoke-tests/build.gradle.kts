plugins {
    `java-library`
}

dependencies {
    implementation(project(":aws-xray-recorder-sdk-core"))
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
