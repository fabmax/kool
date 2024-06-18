plugins {
    id("kool.lib-conventions")
    id("kool.publish-conventions")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":kool-core"))
            implementation(libs.kotlin.coroutines)
        }

        desktopMain.dependencies {
            api(libs.physxjni)
            listOf("natives-linux", "natives-windows", "natives-macos", "natives-macos-arm64").forEach { platform ->
                runtimeOnly("${libs.physxjni.get()}:$platform")
            }
        }

        jsMain.dependencies {
            api(npm(libs.physxjswebidl.get().name, libs.versions.physxjswebidl.get()))
//            api(npm(File("$projectDir/npm/physx-js-webidl")))
        }
    }
}
