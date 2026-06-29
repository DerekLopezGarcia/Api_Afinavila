package es.afinavila.services

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object ComunidadNames {
    private val gson = Gson()
    private var configPath: String = "/data/comunidad_names.json"
    private var loadedMap: Map<String, String> = emptyMap()
    private var lastLoadTime = 0L

    fun setConfigPath(path: String) {
        configPath = path
        reload()
    }

    fun reload() {
        val file = File(configPath)
        if (!file.exists()) {
            loadedMap = emptyMap()
            return
        }

        try {
            val text = file.readText(Charsets.UTF_8)
            val type = object : TypeToken<Map<String, String>>() {}.type
            val fileMap: Map<String, String> = gson.fromJson(text, type)
            loadedMap = fileMap
            lastLoadTime = file.lastModified()
        } catch (e: Exception) {
            loadedMap = emptyMap()
        }
    }

    fun getName(codigoAcceso: String): String {
        val file = File(configPath)
        if (file.exists() && file.lastModified() > lastLoadTime) {
            reload()
        }
        return loadedMap[codigoAcceso] ?: codigoAcceso
    }

    fun containsKey(key: String): Boolean = loadedMap.containsKey(key)

    fun getMap(): Map<String, String> = loadedMap
}
