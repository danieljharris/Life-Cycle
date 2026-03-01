package DrDan.AnimalsGrow.config

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.ExtraInfo
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.array.ArrayCodec
import com.hypixel.hytale.codec.schema.SchemaContext
import com.hypixel.hytale.codec.schema.config.Schema
import com.hypixel.hytale.codec.util.RawJsonReader
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import org.bson.BsonValue

class AnimalsGrowConfig {
    var growsUpInto: MutableList<GrowthEntry> = ArrayList()

    constructor() {
        // 86400 = 1 in-game day (24 real minutes)
        growsUpInto.add(GrowthEntry("Tamed_Sheep_Lamb"          , "Tamed_Sheep"         , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Pig_Piglet"          , "Tamed_Pig"           , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Bison_Calf"          , "Tamed_Bison"         , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Boar_Piglet"         , "Tamed_Boar"          , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Camel_Calf"          , "Tamed_Camel"         , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Chicken_Desert_Chick", "Tamed_Chicken_Desert", 86400))
        growsUpInto.add(GrowthEntry("Tamed_Chicken_Chick"       , "Tamed_Chicken"       , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Cow_Calf"            , "Tamed_Cow"           , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Goat_Kid"            , "Tamed_Goat"          , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Horse_Foal"          , "Tamed_Horse"         , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Mouflon_Lamb"        , "Tamed_Mouflon"       , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Pig_Wild_Piglet"     , "Tamed_Pig_Wild"      , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Bunny"               , "Tamed_Rabbit"        , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Ram_Lamb"            , "Tamed_Ram"           , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Skrill_Chick"        , "Tamed_Skrill"        , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Turkey_Chick"        , "Tamed_Turkey"        , 86400))
        growsUpInto.add(GrowthEntry("Tamed_Warthog_Piglet"      , "Tamed_Warthog"       , 86400))
    }

    private class ListCodec : Codec<MutableList<GrowthEntry>> {
        private val arrayCodec = ArrayCodec(GrowthEntry.CODEC) { size -> arrayOfNulls<GrowthEntry>(size) }

        override fun decode(bsonValue: BsonValue, extraInfo: ExtraInfo): MutableList<GrowthEntry>? {
            val arr = arrayCodec.decode(bsonValue, extraInfo)
            return if (arr != null) arr.toMutableList() else ArrayList()
        }

        override fun encode(list: MutableList<GrowthEntry>, extraInfo: ExtraInfo): BsonValue {
            val arr = list.toTypedArray()
            return arrayCodec.encode(arr, extraInfo)
        }

        override fun decodeJson(reader: RawJsonReader, extraInfo: ExtraInfo): MutableList<GrowthEntry>? {
            val arr = arrayCodec.decodeJson(reader, extraInfo)
            return if (arr != null) arr.toMutableList() else ArrayList()
        }

        override fun toSchema(context: SchemaContext): Schema {
            return arrayCodec.toSchema(context)
        }
    }

    companion object {
        @JvmStatic
        val CODEC = BuilderCodec.builder(AnimalsGrowConfig::class.java, ::AnimalsGrowConfig)
            .versioned()
            .codecVersion(2)
            .append(
                KeyedCodec("GrowsUpInto", ListCodec()),
                { config, value -> config.growsUpInto = value },
                { it.growsUpInto }
            ).setVersionRange(2, 2).add()
            .build()!!
    }
}
