package DrDan.AnimalsBreed.resource_creator

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.asset.AssetModule
import com.hypixel.hytale.common.semver.Semver
import com.hypixel.hytale.common.plugin.PluginManifest

import io.github.evgenius1424.jsonmergepatch.mergePatch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import java.util.Vector
import java.util.concurrent.ConcurrentHashMap
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.FileSystem
import java.nio.file.FileSystems

import DrDan.AnimalsBreed.AnimalsBreed
import DrDan.AnimalsBreed.AnimalsBreedAction

class ResourceCreator {
    val overridePath = "Mods/DrDan_LifeCycle/Override/AnimalsBreed"

    // val target = Json.parseToJsonElement("""{"a": "b", "c": "d"}""")
    // val patch = Json.parseToJsonElement("""{"a": "z", "c": null, "e": "f"}""")
    // val result = target.mergePatch(patch)
    // // {"a": "z", "e": "f"}

    // Ingredient_Fiber
    // {"Modify":{"AttractiveItemSet":["Ingredient_Fiber"]}}
    // Server/NPC/Roles/Creature/Livestock/Tamed/Tamed_Bison.json

    fun removeArrayValues(baseElem: JsonElement, patchElem: JsonElement): JsonElement {
        if (baseElem is JsonObject && patchElem is JsonObject) {
            val resultMap = mutableMapOf<String, JsonElement>()
            // Keep keys from base, but apply removals where patch provides arrays/objects
            for ((key, baseValue) in baseElem) {
                val patchValue = patchElem[key]
                if (patchValue != null) {
                    when {
                        patchValue is JsonArray && baseValue is JsonArray -> {
                            val removeSet = HashSet<JsonElement>()
                            for (e in patchValue) removeSet.add(e)
                            val newArray = buildJsonArray {
                                for (e in baseValue) { if (!removeSet.contains(e)) add(e) }
                            }
                            resultMap[key] = newArray
                        }
                        patchValue is JsonObject && baseValue is JsonObject -> {
                            resultMap[key] = removeArrayValues(baseValue, patchValue)
                        }
                        else -> {
                            // unsupported patch type for this operation — leave base value unchanged
                            resultMap[key] = baseValue
                        }
                    }
                } else {
                    resultMap[key] = baseValue
                }
            }
            return JsonObject(resultMap)
        }
        return baseElem
    }

    // Remove a single value from array
    // base = {"Modify":{"AttractiveItemSet":["Ingredient_Fiber", "Plant_Cabbage"]}} + patch = {"Modify":{"AttractiveItemSet":["Ingredient_Fiber"]}} = {"Modify":{"AttractiveItemSet":["Plant_Cabbage"]}}
    // Needs to first get the existing array, remove the value, then save the modified array back to the JSON
    // The patch contains the value to remove and the path to get to the existing array
    fun mergePatchArrayMove(hytaleAssetsZipPath: Path, jsonFileToGet: Path, patch: String) {
        println("Base Path: $hytaleAssetsZipPath")
        val baseFile = extractAsset(hytaleAssetsZipPath, jsonFileToGet)
        if (baseFile.isBlank()) {
            println("Base file empty or missing: $jsonFileToGet")
            return
        }

        val target = Json.parseToJsonElement(baseFile)
        val patchElement = Json.parseToJsonElement(patch)

        val result = removeArrayValues(target, patchElement)
        save(jsonFileToGet.toString(), Json.encodeToString(result))
        registerPack()
    }

    // https://github.com/evgenius1424/kotlin-json-merge-patch
    // JSON Merge Patch https://datatracker.ietf.org/doc/html/rfc7386 / https://www.rfc-editor.org/rfc/rfc7396.txt
    fun mergePatch(hytaleAssetsZipPath: Path, jsonFileToGet: Path, patch: String) {
        println("Base Path: $hytaleAssetsZipPath")
        val baseFile = extractAsset(hytaleAssetsZipPath, jsonFileToGet)
        // println("Base File Contents: $baseFile")
        val target = Json.parseToJsonElement(baseFile)
        val patchElement = Json.parseToJsonElement(patch)
        val result = target.mergePatch(patchElement)
        // println("Merged Result:\n$result")
        save(jsonFileToGet.toString(), Json.encodeToString(result))
        registerPack()
    }

    fun save(fileName: String, content: String) {
        val pathName = "$overridePath/$fileName"
        try {
            val path = Paths.get(pathName)
            Files.createDirectories(path.parent)
            Files.writeString(path, content)
            println("Saved merged JSON to $path")
        } catch (e: java.io.IOException) {
            throw RuntimeException(e)
        }
    }

    fun registerPack() {
        val path = Paths.get(overridePath)

        // TODO: Get this from /workspace/plugin/manifest/constants.bzl
        val manifest = PluginManifest(
            "DrDan",                        // group
            "Overrides",                    // name
            Semver.fromString("1.0.0"), // version
            "Asset overrides",              // description
            mutableListOf(),                // authors
            "",                             // website
            null,                           // main
            "2026.02.18-f3b8fff95",         // serverVersion
            mutableMapOf(),                 // dependencies
            mutableMapOf(),                 // optionalDependencies
            mutableMapOf(),                // loadBefore
            mutableListOf(),               // subPlugins
            false                          // disabledByDefault
        )

        // try{
        //     AssetModule.get().unregisterPack("DrDan:Overrides")
        // } catch (e: Exception) {
        //     println("No existing pack to unregister, proceeding with registration")
        // }
        // AssetModule.get().registerPack("DrDan:Overrides", path, manifest, false)
        AssetModule.get().registerPack("DrDan:Overrides", path, manifest, true)

        println("Registered asset pack from $path")
    }

    fun extractAsset(hytaleAssetsZipPath: Path, jsonFileToGet: Path): String {
        val zipFs = FileSystems.newFileSystem(hytaleAssetsZipPath, emptyMap<String, Any>())
        var content: String = ""
        try {
            val target: Path = zipFs.getPath(jsonFileToGet.toString())
            if (Files.exists(target)) {
                try {
                    val bytes = Files.readAllBytes(target)
                    content = String(bytes)
                    // println("Contents of $jsonFileToGet:\n")
                    // println(content)
                } catch (e: Exception) {
                    println("Failed reading $jsonFileToGet from ZIP: ${e.message}")
                }
            } else {
                println("JSON file not found in asset ZIP: $jsonFileToGet")
            }
        } finally {
            zipFs.close()
        }

        return content
    }

    fun listFilesInZipPath(hytaleAssetsZipPath: Path, dirPath: String = "Server/NPC/Roles/Creature/Livestock/Tamed"): Vector<Path> {
        val zipFs = FileSystems.newFileSystem(hytaleAssetsZipPath, emptyMap<String, Any>())
        val result = Vector<Path>()
        try {
            val base = zipFs.getPath(dirPath)
            if (Files.exists(base)) {
                Files.walk(base).use { stream ->
                    stream.filter { Files.isRegularFile(it) }.forEach { result.add(it) }
                }
            } else {
                println("Directory not found in ZIP: $dirPath")
            }
        } finally {
            zipFs.close()
        }
        return result
    }
}