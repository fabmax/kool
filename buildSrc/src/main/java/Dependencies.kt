import org.gradle.internal.os.OperatingSystem

object Versions {
    val kotlinVersion = "1.8.20"
    val kotlinCoroutinesVersion = "1.7.0-Beta"
    val kotlinSerializationVersion = "1.5.0"
}

object DepsCommon {
    val kotlinCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutinesVersion}"
    val kotlinSerialization = "org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.kotlinSerializationVersion}"
    val kotlinSerializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinSerializationVersion}"
}

object DepsJvm {
    val lwjglVersion = "3.3.2"
    val nativeLibsSuffix = OperatingSystem.current().let {
        when {
            it.isLinux -> "natives-linux"
            it.isMacOsX -> "natives-macos"
            else -> "natives-windows"
        }
    }

    val physxJniVersion = "2.0.5"
    val physxJni = "de.fabmax:physx-jni:${physxJniVersion}"
    val physxJniRuntime = "de.fabmax:physx-jni:${physxJniVersion}:${nativeLibsSuffix}"

    fun lwjgl(subLib: String? = null): String {
        return if (subLib != null) {
            "org.lwjgl:lwjgl-$subLib:${lwjglVersion}"
        } else {
            "org.lwjgl:lwjgl:${lwjglVersion}"
        }
    }

    fun lwjglNatives(subLib: String? = null): String {
        return if (subLib != null) {
            "org.lwjgl:lwjgl-$subLib:${lwjglVersion}:${nativeLibsSuffix}"
        } else {
            "org.lwjgl:lwjgl:${lwjglVersion}:${nativeLibsSuffix}"
        }
    }
}
