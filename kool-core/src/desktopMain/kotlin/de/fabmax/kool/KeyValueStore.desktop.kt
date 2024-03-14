package de.fabmax.kool

import de.fabmax.kool.util.Uint8Buffer
import de.fabmax.kool.util.Uint8BufferImpl
import de.fabmax.kool.util.logE
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread

internal actual fun PlatformKeyValueStore(): PlatformKeyValueStore = JvmKeyValueStore

object JvmKeyValueStore : PlatformKeyValueStore {

    private const val KEY_VALUE_STORAGE_NAME = ".keyValueStorage.json"

    private val storageDir: File = File(KoolSystem.configJvm.storageDir)
    private val keyValueStore = mutableMapOf<String, String>()

    init {
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            logE { "Failed to create storage directory" }
        }

        val persistentKvStorage = File(storageDir, KEY_VALUE_STORAGE_NAME)
        if (persistentKvStorage.canRead()) {
            try {
                val kvStore = Json.Default.decodeFromString(KeyValueStore.serializer(), persistentKvStorage.readText())
                kvStore.keyValues.forEach { (k, v) -> keyValueStore[k] = v }
            } catch (e: Exception) {
                logE { "Failed loading key value store: $e" }
                e.printStackTrace()
            }
        }

        Runtime.getRuntime().addShutdownHook(thread(false) {
            val kvStore = KeyValueStore(keyValueStore.map { (k, v) -> KeyValueEntry(k, v) })
            File(storageDir, KEY_VALUE_STORAGE_NAME).writeText(Json.encodeToString(kvStore))
        })
    }

    override fun storageKeys(): Set<String> {
        val keys = mutableSetOf<String>()
        keys += keyValueStore.keys
        storageDir.list()?.let {
            keys += it
        }
        return keys
    }

    override fun storeBlob(key: String, data: Uint8Buffer): Boolean {
        return try {
            FileOutputStream(File(storageDir, key)).use { it.write(data.toArray()) }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    override fun storeString(key: String, data: String): Boolean {
        keyValueStore[key] = data
        return true
    }

    override fun loadBlob(key: String): Uint8Buffer? {
        val file = File(storageDir, key)
        if (!file.canRead()) {
            return null
        }
        return try {
            Uint8BufferImpl(file.readBytes())
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun loadString(key: String): String? {
        return keyValueStore[key]
    }

    override fun delete(key: String) {
        keyValueStore.remove(key)
        val f = File(storageDir, key)
        if (f.exists()) {
            f.delete()
        }
    }

    @Serializable
    private data class KeyValueEntry(val k: String, val v: String)

    @Serializable
    private data class KeyValueStore(val keyValues: List<KeyValueEntry>)
}
