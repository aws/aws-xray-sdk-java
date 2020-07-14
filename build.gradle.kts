import net.ltgt.gradle.errorprone.errorprone
import nl.javadude.gradle.plugins.license.LicenseExtension
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("com.github.hierynomus.license") apply false
    id("net.ltgt.errorprone") apply false
    id("org.checkerframework") apply false

    id("org.ajoberstar.grgit")
    id("org.ajoberstar.reckon")
}

reckon {
    scopeFromProp()
    snapshotFromProp()
}

val prepareRelease = tasks.register("prepareRelease") {
    doLast {
        val readmeText = file("README.md").readText()
        val updatedText = readmeText.replace("<version>[^<]+<\\/version>".toRegex(), "<version>${project.version}</version>")
        file("README.md").writeText(updatedText)

        grgit.commit(mapOf("message" to "Releasing ${project.version}", "all" to true))
    }
}

val release = tasks.register("release") {
    dependsOn(prepareRelease)
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
            add("testImplementation", "junit:junit")
            add("testImplementation", "org.assertj:assertj-core")
            add("testImplementation", "org.mockito:mockito-core")

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

        prepareRelease.configure {
            dependsOn(tasks.named("build"))
        }

        val publish = tasks.named("publish")
        publish.configure {
            mustRunAfter(prepareRelease)
        }

        release.configure {
            dependsOn(publish)
        }

        val isSnapshot = version.toString().endsWith("SNAPSHOT")

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

            repositories {
                maven {
                    val repoUrlBase = "https://aws.oss.sonatype.org/content/repositories"
                    url = uri("$repoUrlBase/${if (isSnapshot) "snapshots" else "releases"}")
                    credentials {
                        username = "${findProperty("aws.sonatype.username")}"
                        password = "${findProperty("aws.sonatype.password")}"
                    }
                }
            }
        }

        tasks.withType<Sign>().configureEach {
            onlyIf { !isSnapshot }
        }

        configure<SigningExtension> {
            useGpgCmd()
            sign(the<PublishingExtension>().publications["maven"])
        }
    }
}
