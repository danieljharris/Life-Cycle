package DrDan.AnimalsBreed.config

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec

class BreedEntry {
    var breedingGroup: Array<String>? = null

    constructor()

    constructor(breedingGroup: Array<String>?) {
        this.breedingGroup = breedingGroup
    }

    companion object {
        @JvmStatic
        val CODEC = BuilderCodec.builder(BreedEntry::class.java, ::BreedEntry)
            .append(KeyedCodec("BreedingGroup", Codec.STRING_ARRAY), { entry, value -> entry.breedingGroup = value }, { it.breedingGroup }).add()
            .build()!!
    }
}
