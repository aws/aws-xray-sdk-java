plugins {
    `java-platform`
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val DEPENDENCY_BOMS = listOf(
        "com.fasterxml.jackson:jackson-bom:2.11.0",
        "org.junit:junit-bom:5.6.2"
)

val DEPENDENCY_SETS = listOf(
        DependencySet(
                "com.google.errorprone",
                "2.5.1",
                listOf("error_prone_annotations")
        ),
        DependencySet(
                "com.fasterxml.jackson.datatype",
                "2.11.0",
                listOf("jackson-datatype-jsr310")
        ),
        DependencySet(
                "com.github.tomakehurst",
                "2.26.3",
                listOf("wiremock-jre8")
        ),
        DependencySet(
                "com.google.code.findbugs",
                "3.0.2",
                listOf("jsr305")
        ),
        DependencySet(
                "org.assertj",
                "3.16.1",
                listOf("assertj-core")
        ),
        DependencySet(
                "org.junit-pioneer",
                "0.9.0",
                listOf("junit-pioneer")
        ),
        DependencySet(
                "junit",
                "4.12",
                listOf("junit")
        ),
        DependencySet(
                "org.mockito",
                "3.6.0",
                listOf("mockito-all", "mockito-core", "mockito-junit-jupiter")
        )
)

javaPlatform {
    allowDependencies()
}

dependencies {
    for (bom in DEPENDENCY_BOMS) {
        api(platform(bom))
    }
    constraints {
        for (set in DEPENDENCY_SETS) {
            for (module in set.modules) {
                api("${set.group}:${module}:${set.version}")
            }
        }
    }
}
