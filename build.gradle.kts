plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "1.0.3"

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
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.92.9+1.20.1")

            dependencies {
                modCompileOnly("curse.maven:modmenu-308702:5162837")
            }

            publishingDependencies {
                requires("fabric-api")
                optional("modmenu")
            }
        }
        forge {
            loaderVersion = "47.4.10"
        }
    }

    version("1.21.1") {
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.116.12+1.21.1")

            dependencies {
                modCompileOnly("curse.maven:modmenu-308702:7808443")
            }

            publishingDependencies {
                requires("fabric-api")
                optional("modmenu")
            }
        }
        neoforge {
            loaderVersion = "21.1.228"
        }
    }

    version("1.21.11") {
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.141.4+1.21.11")

            dependencies {
                modCompileOnly("curse.maven:modmenu-308702:7808841")
            }

            publishingDependencies {
                requires("fabric-api")
                optional("modmenu")
            }
        }
        neoforge {
            loaderVersion = "21.11.42"
        }
    }

    version("26.1.2") {
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.148.0+26.1.2")

            dependencies {
                implementation("curse.maven:modmenu-308702:8065321")
            }

            publishingDependencies {
                requires("fabric-api")
                optional("modmenu")
            }
        }
        neoforge {
            loaderVersion = "26.1.2.41-beta"
        }
    }

}
