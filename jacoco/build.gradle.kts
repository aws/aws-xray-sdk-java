plugins {
    `java-library`
}

description = "AWS X-Ray SDK Code Coverage"

dependencies {
    rootProject.subprojects.forEach { subproject ->
        // Generate aggregate coverage report for published modules that enable jacoco.
        subproject.plugins.withId("jacoco") {
            subproject.plugins.withId("maven-publish") {
                implementation(project(subproject.path)) {
                    isTransitive = false
                }
            }
        }
    }
}

val sourcesPath by configurations.creating {
    isVisible = false
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(configurations.implementation.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("source-folders"))
    }
}

val coverageDataPath by configurations.creating {
    isVisible = false
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(configurations.implementation.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("jacoco-coverage-data"))
    }
}

// Task to gather code coverage from multiple subprojects
val codeCoverageReport by tasks.registering(JacocoReport::class) {
    additionalClassDirs(configurations.runtimeClasspath.get())
    additionalSourceDirs(sourcesPath.incoming.artifactView { lenient(true) }.files)
    executionData(coverageDataPath.incoming.artifactView { lenient(true) }.files.filter { it.exists() })

    reports {
        // xml is usually used to integrate code coverage with
        // other tools like SonarQube, Coveralls or Codecov
        xml.isEnabled = true

        // HTML reports can be used to see code coverage
        // without any external tools
        html.isEnabled = true
    }
}

