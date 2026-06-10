plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.zhujian.reader.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageName = "NovelReaderPro"
            packageVersion = "0.3.0"
            windows {
                menuGroup = "NovelReaderPro"
                upgradeUuid = "f4b61830-0385-4af2-93f7-9b8e44d40c16"
            }
        }
    }
}
