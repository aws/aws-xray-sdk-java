import net.ltgt.gradle.errorprone.errorprone

import nl.javadude.gradle.plugins.license.LicenseExtension


plugins {
    id("com.github.hierynomus.license") apply false
    id("net.ltgt.errorprone") apply false
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

        dependencies {
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
                    name = "staging-repo"
                    url = uri("https://aws.oss.sonatype.org/service/local/staging/deploy/maven2")
                }
            }
        }
    }
}
