plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "1.0.0"

prism {
    metadata {
        modId = "twemoji"
        name = "Twemoji"
        description = "Pixelated Twemoji emojis in chat — with a picker, shortcodes, animated emojis, and datapack support."
        license = "All Rights Reserved"
    }

    obfuscate()

    curseMaven()

    version("26.1.2") {
        fabric {
            loaderVersion = "0.19.2"
            fabricApi("0.148.0+26.1.2")

            dependencies {
                implementation("curse.maven:modmenu-308702:8065321")
            }
        }
        neoforge {
            loaderVersion = "26.1.2.41-beta"
        }
    }

}
