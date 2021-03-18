import nebula.plugin.release.git.opinion.Strategies
import net.ltgt.gradle.errorprone.errorprone
import nl.javadude.gradle.plugins.license.LicenseExtension
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("com.github.hierynomus.license") apply false
    id("net.ltgt.errorprone") apply false
    id("org.checkerframework") apply false

    id("nebula.release")
    id("org.ajoberstar.grgit")
    id("io.freefair.aggregate-javadoc-jar")
    id("io.github.gradle-nexus.publish-plugin")
}

tasks {
    val prepareRelease by registering {
        doLast {
            val readmeText = file("README.md").readText()
            val updatedText = readmeText.replace("<version>[^<]+<\\/version>".toRegex(), "<version>${project.version}</version>")
            file("README.md").writeText(updatedText)

            grgit.commit(mapOf("message" to "Releasing ${project.version}", "all" to true))
        }
    }

    named("finalSetup") {
        dependsOn(prepareRelease)
    }
}

val releaseTask = tasks.named("release")
releaseTask.configure {
    mustRunAfter(tasks.named("snapshotSetup"), tasks.named("finalSetup"))
}

release {
    defaultVersionStrategy = Strategies.getSNAPSHOT()
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://aws.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://aws.oss.sonatype.org/content/repositories/snapshots/"))
            username.set("${findProperty("aws.sonatype.username") ?: System.getenv("SONATYPE_USERNAME")}")
            password.set("${findProperty("aws.sonatype.password") ?: System.getenv("SONATYPE_PASSWORD")}")
        }
    }
}

allprojects {
    group = "com.amazonaws"

    repositories {
        mavenCentral()
        mavenLocal()
    }

    plugins.apply("com.github.hierynomus.license")
    configure<LicenseExtension> {
        header = file("${rootProject.projectDir}/config/license/header.txt")

        headerDefinitions {
            // Same as SLASHSTAR_STYLE but with newline at end to match published IntelliJ copyright style.
            register("JAVA_STYLE") {
                // Adds the ending newline.
                endLine   = " */\n"

                // All other config copied from here
                // https://github.com/mycila/license-maven-plugin/blob/bdef2dca8f27af4f3134e03de0aa72d8d0863f99/license-maven-plugin/src/main/java/com/mycila/maven/plugin/license/header/HeaderType.java#L45
                firstLine = "/*"
                beforeEachLine = " * "
                firstLineDetectionPattern = "(\\s|\\t)*/\\*.*$"
                lastLineDetectionPattern  = ".*\\*/(\\s|\\t)*$"
                allowBlankLines = false
                isMultiline = false
                padLines = false
            }
        }

        mapping("java", "JAVA_STYLE")

        exclude("**/*.json")
    }

    plugins.withId("java-library") {
        plugins.apply("checkstyle")
        plugins.apply("net.ltgt.errorprone")
        plugins.apply("org.checkerframework")

        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8

            withJavadocJar()
            withSourcesJar()
        }

        val propertiesDir = "build/generated/sources/properties"

        configure<JavaPluginConvention> {
            sourceSets {
                named("main") {
                    output.dir(mapOf("builtBy" to "generateProperties"), propertiesDir)
                }
            }
        }

        configure<CheckerFrameworkExtension> {
            checkers = listOf("org.checkerframework.checker.nullness.NullnessChecker")

            extraJavacArgs = listOf(
                    "-AsuppressWarnings=type.anno.before.modifier"
            )

            excludeTests = true

            // TODO(anuraaga): Enable on all projects.
            skipCheckerFramework = project.name != "aws-xray-recorder-sdk-core" || JavaVersion.current() != JavaVersion.VERSION_11
        }

        dependencies {
            add("testImplementation", "org.junit.jupiter:junit-jupiter-api")
            add("testImplementation", "org.junit-pioneer:junit-pioneer")
            add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine")
            add("testRuntimeOnly", "org.junit.vintage:junit-vintage-engine")
            add("testImplementation", "junit:junit")

            add("testImplementation", "org.assertj:assertj-core")
            add("testImplementation", "org.mockito:mockito-core")
            add("testImplementation", "org.mockito:mockito-junit-jupiter")

            add("compileOnly", "com.google.errorprone:error_prone_annotations")

            add("compileOnly", "org.checkerframework:checker-qual:3.4.1")
            add("testImplementation", "org.checkerframework:checker-qual:3.4.1")
            add("checkerFramework", "org.checkerframework:checker:3.4.1")


            add("errorprone", "com.google.errorprone:error_prone_core:2.4.0")
            if (!JavaVersion.current().isJava9Compatible) {
                add("errorproneJavac", "com.google.errorprone:javac:9+181-r4173-1")
            }

            configurations.configureEach {
                if (isCanBeResolved && !isCanBeConsumed) {
                    add(name, platform(project(":dependencyManagement")))
                }
            }
        }

        tasks {
            withType<JavaCompile> {
                options.errorprone {
                    error(
                            "AssertionFailureIgnored",
                            "BadInstanceof",
                            "BoxedPrimitiveConstructor",
                            "CatchFail",
                            "DefaultCharset",
                            "InheritDoc",
                            "MathAbsoluteRandom",
                            "MissingOverride",
                            "UnnecessaryParentheses"
                    )

                    // TODO(anuraaga): These will improve the Javadoc but punt for now since it's a lot of work and not
                    // as important as code checks.
                    disable(
                            "EmptyBlockTag",
                            "MissingSummary"
                    )

                    // TODO(anuraaga): We don't have a dependency on Guava or similar so for now disable this. At least
                    // hot code paths should not be calling split though and we would probably re-enable it and suppress
                    // when needed instead.
                    disable(
                            "StringSplitter"
                    )

                    // TODO(anuraaga): We have Date in some APIs, revisit this separately.
                    disable("JdkObsolete")
                }
            }

            withType<Test> {
                useJUnitPlatform()

                testLogging {
                    exceptionFormat = TestExceptionFormat.FULL
                    showStackTraces = true
                }
            }

            named<Javadoc>("javadoc") {
                val options = options as StandardJavadocDocletOptions

                options.quiet()
                options.addBooleanOption("-allow-script-in-comments", true)
                options.footer = "<script src=\"/SdkStatic/sdk-priv.js\" async=\"true\"></script>"
                options.bottom = "Copyright &#169; 2018 Amazon Web Services, Inc. All Rights Reserved."

                // TODO(anuraaga): Enable doclint except for required @param/@returns
                options.addBooleanOption("Xdoclint:none", true)
                // options.addBooleanOption("Xdoclint:accessibility", true)
                // options.addBooleanOption("Xdoclint:html", true)
                // options.addBooleanOption("Xdoclint:reference", true)
                // options.addBooleanOption("Xdoclint:syntax", true)
            }

            val generateProperties by registering {
                doLast {
                    val folder = file("${propertiesDir}/com/amazonaws/xray")
                    folder.mkdirs()
                    val propertiesFile = folder.resolve("sdk.properties")
                    propertiesFile.writeText("awsxrayrecordersdk.version=${project.version}")
                }
            }
        }
    }

    plugins.withId("maven-publish") {
        plugins.apply("signing")

        releaseTask.configure {
            finalizedBy(tasks.named("publishToSonatype"))
        }

        // Don't publish Gradle metadata for now until verifying they work well.
        tasks.withType<GenerateModuleMetadata> {
            enabled = false
        }

        configure<PublishingExtension> {
            publications {
                register<MavenPublication>("maven") {
                    plugins.withId("java-platform") {
                        from(components["javaPlatform"])
                    }
                    plugins.withId("java-library") {
                        from(components["java"])
                    }
                    
                    versionMapping {
                        allVariants {
                            fromResolutionResult()
                        }
                    }
                    pom {
                        afterEvaluate {
                            pom.name.set(project.description)
                        }
                        description.set("The Amazon Web Services X-Ray Recorder SDK for Java provides Java APIs for " +
                                "emitting tracing data to AWS X-Ray. AWS X-Ray helps developers analyze and debug " +
                                "distributed applications. With X-Ray, you can understand how your application and " +
                                "its underlying services are performing to identify and troubleshoot the root cause of " +
                                "performance issues and errors.")
                        url.set("https://aws.amazon.com/documentation/xray/")


                        licenses {
                            license {
                                name.set("Apache License, Version 2.0")
                                url.set("https://aws.amazon.com/apache2.0")
                                distribution.set("repo")
                            }
                        }

                        developers {
                            developer {
                                id.set("amazonwebservices")
                                organization.set("Amazon Web Services")
                                organizationUrl.set("https://aws.amazon.com")
                                roles.add("developer")
                            }
                        }

                        scm {
                            url.set("https://github.com/aws/aws-xray-sdk-java.git")
                        }

                        properties.put("awsxrayrecordersdk.version", project.version.toString())
                    }
                }
            }
        }

        tasks.withType<Sign>().configureEach {
            onlyIf { System.getenv("CI") == "true" }
        }

        configure<SigningExtension> {
            val signingKeyId = System.getenv("GPG_KEY_ID")
            val signingKey = System.getenv("GPG_PRIVATE_KEY")
            val signingPassword = System.getenv("GPG_PASSWORD")
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            sign(the<PublishingExtension>().publications["maven"])
        }
    }
}

subprojects {
    group = "com.amazonaws"

    plugins.withId("java-library") {
        plugins.apply("jacoco")

        configure<JacocoPluginExtension> {
            toolVersion = "0.8.6"
        }

        // Do not generate reports for individual projects
        tasks.named("jacocoTestReport") {
            enabled = false
        }

        configurations {
            val implementation by getting

            create("transitiveSourceElements") {
                isVisible = false
                isCanBeResolved = false
                isCanBeConsumed = true
                extendsFrom(implementation)
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                    attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("source-folders"))
                }
                val mainSources = the<JavaPluginConvention>().sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                mainSources.java.srcDirs.forEach {
                    outgoing.artifact(it)
                }
            }

            create("coverageDataElements") {
                isVisible = false
                isCanBeResolved = false
                isCanBeConsumed = true
                extendsFrom(implementation)
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                    attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("jacoco-coverage-data"))
                }
                // This will cause the test task to run if the coverage data is requested by the aggregation task
                tasks.withType(Test::class) {
                    outgoing.artifact(extensions.getByType<JacocoTaskExtension>().destinationFile!!)
                }
            }

            configureEach {
                resolutionStrategy {
                    failOnVersionConflict()
                    preferProjectModules()
                }
            }
        }
    }
}
