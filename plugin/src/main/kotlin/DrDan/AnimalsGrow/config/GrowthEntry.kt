package DrDan.AnimalsGrow.config

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec

class GrowthEntry {
    var baby: String? = null
    var adult: String? = null
    var timeToGrowUpSeconds: Int? = null

    constructor()

    constructor(baby: String?, adult: String?, timeToGrowUpSeconds: Int?) {
        this.baby = baby
        this.adult = adult
        this.timeToGrowUpSeconds = timeToGrowUpSeconds
    }

    companion object {
        @JvmStatic
        val CODEC = BuilderCodec.builder(GrowthEntry::class.java, ::GrowthEntry)
            .append(KeyedCodec("Baby", Codec.STRING), { entry, value -> entry.baby = value }, { it.baby }).add()
            .append(KeyedCodec("Adult", Codec.STRING), { entry, value -> entry.adult = value }, { it.adult }).add()
            .append(KeyedCodec("TimeToGrowUpSeconds", Codec.INTEGER), { entry, value -> entry.timeToGrowUpSeconds = value }, { it.timeToGrowUpSeconds }).add()
            .build()!!
    }
}
