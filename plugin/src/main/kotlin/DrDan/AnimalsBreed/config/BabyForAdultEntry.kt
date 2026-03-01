package DrDan.AnimalsBreed.config

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.Codec

class BabyForAdultEntry {
    var adult: String? = null
    var baby: String? = null

    constructor()

    constructor(adult: String?, baby: String?) {
        this.adult = adult
        this.baby = baby
    }

    companion object {
        @JvmStatic
        val CODEC = BuilderCodec.builder(BabyForAdultEntry::class.java, ::BabyForAdultEntry)
            .append(KeyedCodec("Adult", Codec.STRING), { e, v -> e.adult = v }, { it.adult }).add()
            .append(KeyedCodec("Baby", Codec.STRING), { e, v -> e.baby = v }, { it.baby }).add()
            .build()!!
    }
}
