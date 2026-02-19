package DrDan.AnimalsBreed.resource_creator

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent

import io.github.evgenius1424.jsonmergepatch.mergePatch
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.FileSystem
import java.nio.file.FileSystems

import DrDan.AnimalsBreed.AnimalsBreed
import DrDan.AnimalsBreed.AnimalsBreedAction

class ResourceCreator {
    val overridePath = "Server/Override/AnimalsBreed"

    // val target = Json.parseToJsonElement("""{"a": "b", "c": "d"}""")
    // val patch = Json.parseToJsonElement("""{"a": "z", "c": null, "e": "f"}""")
    // val result = target.mergePatch(patch)
    // // {"a": "z", "e": "f"}

    // Ingredient_Fiber
    // {"Modify":{"AttractiveItemSet":"Ingredient_Fiber"}}
    // Server/NPC/Roles/Creature/Livestock/Tamed/Tamed_Bison.json

    // JSON Merge Patch https://datatracker.ietf.org/doc/html/rfc7386 / https://www.rfc-editor.org/rfc/rfc7396.txt
    fun mergePatch(hytaleAssetsZipPath: Path, jsonFileToGet: Path, patch: String) {
        println("Base Path: $hytaleAssetsZipPath")
        val baseFile = extractAsset(hytaleAssetsZipPath, jsonFileToGet)
        // println("Base File Contents: $baseFile")
        val target = Json.parseToJsonElement(baseFile)
        val patchElement = Json.parseToJsonElement(patch)
        val result = target.mergePatch(patchElement)
        println("Merged Result:\n$result")
        // save(hytaleAssetPath.fileName.toString(), Json.encodeToString(result))
    }

    fun save(fileName: String, content: String) {
        val fullPath = "$overridePath/$fileName"
        try {
            val path = Paths.get(fullPath)
            Files.createDirectories(path.parent)
            Files.writeString(path, content)
        } catch (e: java.io.IOException) {
            throw RuntimeException(e)
        }
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
}