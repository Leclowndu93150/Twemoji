plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "1.0.6"

prism {
    metadata {
        modId = "twemoji"
        name = "Twemoji"
        description = "Pixelated Twemoji emojis in chat — with a picker, shortcodes, animated emojis, and datapack support."
        license = "All Rights Reserved"
    }

//    skidfuscate()
//    obfuscate()

    curseMaven()
    maven("Leclown", "https://maven.leclowndu93150.dev/releases")
    maven("Xander", "https://maven.isxander.dev/releases")

    publishing {
        changelogFile = "CHANGELOG.md"

        curseforge {
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            projectId = "1543001"
        }

        modrinth {
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            projectId = "aCiQlU58"
        }
    }

    version("1.20.1") {
        common {
            modCompileOnly("com.leclowndu93150.baguettelib:baguettelib-1.20.1-common:2.0.5")
            modCompileOnly("dev.isxander:yet-another-config-lib:3.6.6+1.20.1-forge")
        }
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.92.9+1.20.1")

            dependencies {
                modImplementation("com.leclowndu93150.baguettelib:baguettelib-1.20.1-fabric:2.0.5")
                modCompileOnly("dev.isxander:yet-another-config-lib:3.6.6+1.20.1-fabric")
                modRuntimeOnly("dev.isxander:yet-another-config-lib:3.6.6+1.20.1-fabric")
                modCompileOnly("curse.maven:modmenu-308702:5162837")
                modRuntimeOnly("curse.maven:chat-heads-407206:8244418")
            }

            publishingDependencies {
                requires("fabric-api")
                requires("baguettelib")
                requires("yacl")
                optional("modmenu")
                optional("chat-heads")
            }
        }
        forge {
            loaderVersion = "47.4.10"
            dependencies {
                modImplementation("com.leclowndu93150.baguettelib:baguettelib-1.20.1-forge:2.0.5")
                modCompileOnly("dev.isxander:yet-another-config-lib:3.6.6+1.20.1-forge")
                modRuntimeOnly("dev.isxander:yet-another-config-lib:3.6.6+1.20.1-forge")
                modRuntimeOnly("curse.maven:chat-heads-407206:8244416")
            }

            publishingDependencies {
                requires("baguettelib")
                requires("yacl")
            }
        }
    }

    version("1.21.1") {
        common {
            modCompileOnly("com.leclowndu93150.baguettelib:baguettelib-1.21.1-common:2.0.5")
            modCompileOnly("dev.isxander:yet-another-config-lib:3.8.1+1.21.1-neoforge")
        }
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.116.12+1.21.1")

            dependencies {
                modImplementation("com.leclowndu93150.baguettelib:baguettelib-1.21.1-fabric:2.0.5")
                modCompileOnly("dev.isxander:yet-another-config-lib:3.8.1+1.21.1-fabric")
                modRuntimeOnly("dev.isxander:yet-another-config-lib:3.8.1+1.21.1-fabric")
                modCompileOnly("curse.maven:modmenu-308702:7808443")
                modRuntimeOnly("curse.maven:chat-heads-407206:8244450")
            }

            publishingDependencies {
                requires("fabric-api")
                requires("baguettelib")
                requires("yacl")
                optional("modmenu")
                optional("chat-heads")
            }
        }
        neoforge {
            loaderVersion = "21.1.228"
            dependencies {
                modImplementation("com.leclowndu93150.baguettelib:baguettelib-1.21.1-neoforge:2.0.5")
                modCompileOnly("dev.isxander:yet-another-config-lib:3.8.1+1.21.1-neoforge")
                modRuntimeOnly("dev.isxander:yet-another-config-lib:3.8.1+1.21.1-neoforge")
                modRuntimeOnly("curse.maven:chat-heads-407206:8244448")
            }

            publishingDependencies {
                requires("baguettelib")
                requires("yacl")
            }
        }
    }

    version("1.21.11") {
        common {
            modCompileOnly("com.leclowndu93150.baguettelib:baguettelib-1.21.11-common:2.0.5")
            modCompileOnly("dev.isxander:yet-another-config-lib:3.8.1+1.21.11-neoforge")
        }
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.141.4+1.21.11")

            dependencies {
                modImplementation("com.leclowndu93150.baguettelib:baguettelib-1.21.11-fabric:2.0.5")
                modCompileOnly("dev.isxander:yet-another-config-lib:3.8.1+1.21.11-fabric")
                modRuntimeOnly("dev.isxander:yet-another-config-lib:3.8.1+1.21.11-fabric")
                modCompileOnly("curse.maven:modmenu-308702:7808841")
                modRuntimeOnly("curse.maven:chat-heads-407206:8174612")
            }

            publishingDependencies {
                requires("fabric-api")
                requires("baguettelib")
                requires("yacl")
                optional("modmenu")
                optional("chat-heads")
            }
        }
        neoforge {
            loaderVersion = "21.11.42"
            dependencies {
                modImplementation("com.leclowndu93150.baguettelib:baguettelib-1.21.11-neoforge:2.0.5")
                modCompileOnly("dev.isxander:yet-another-config-lib:3.8.1+1.21.11-neoforge")
                modRuntimeOnly("dev.isxander:yet-another-config-lib:3.8.1+1.21.11-neoforge")
                modRuntimeOnly("curse.maven:chat-heads-407206:8174609")
            }

            publishingDependencies {
                requires("baguettelib")
                requires("yacl")
            }
        }
    }

    version("26.1.2") {
        common {
            modCompileOnly("com.leclowndu93150.baguettelib:baguettelib-26.1.2-common:2.0.5")
            modCompileOnly("dev.isxander:yet-another-config-lib:3.9.6+26.1-neoforge")
        }
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.148.0+26.1.2")

            dependencies {
                modImplementation("com.leclowndu93150.baguettelib:baguettelib-26.1.2-fabric:2.0.5")
                modCompileOnly("dev.isxander:yet-another-config-lib:3.9.6+26.1-fabric")
                modRuntimeOnly("dev.isxander:yet-another-config-lib:3.9.6+26.1-fabric")
                implementation("curse.maven:modmenu-308702:8065321")
                modRuntimeOnly("curse.maven:chat-heads-407206:8174615")
            }

            publishingDependencies {
                requires("fabric-api")
                requires("baguettelib")
                requires("yacl")
                optional("modmenu")
                optional("chat-heads")
            }
        }
        neoforge {
            loaderVersion = "26.1.2.41-beta"
            dependencies {
                modImplementation("com.leclowndu93150.baguettelib:baguettelib-26.1.2-neoforge:2.0.5")
                modCompileOnly("dev.isxander:yet-another-config-lib:3.9.6+26.1-neoforge")
                modRuntimeOnly("dev.isxander:yet-another-config-lib:3.9.6+26.1-neoforge")
                modRuntimeOnly("curse.maven:chat-heads-407206:8174614")
            }

            publishingDependencies {
                requires("baguettelib")
                requires("yacl")
            }
        }
    }

}
