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
        growsUpInto.add(GrowthEntry("Sheep_Lamb"          , "Sheep"         , 86400))
        growsUpInto.add(GrowthEntry("Pig_Piglet"          , "Pig"           , 86400))
        growsUpInto.add(GrowthEntry("Bison_Calf"          , "Bison"         , 86400))
        growsUpInto.add(GrowthEntry("Boar_Piglet"         , "Boar"          , 86400))
        growsUpInto.add(GrowthEntry("Camel_Calf"          , "Camel"         , 86400))
        growsUpInto.add(GrowthEntry("Chicken_Desert_Chick", "Chicken_Desert", 86400))
        growsUpInto.add(GrowthEntry("Chicken_Chick"       , "Chicken"       , 86400))
        growsUpInto.add(GrowthEntry("Cow_Calf"            , "Cow"           , 86400))
        growsUpInto.add(GrowthEntry("Goat_Kid"            , "Goat"          , 86400))
        growsUpInto.add(GrowthEntry("Horse_Foal"          , "Horse"         , 86400))
        growsUpInto.add(GrowthEntry("Mouflon_Lamb"        , "Mouflon"       , 86400))
        growsUpInto.add(GrowthEntry("Pig_Wild_Piglet"     , "Pig_Wild"      , 86400))
        growsUpInto.add(GrowthEntry("Bunny"               , "Rabbit"        , 86400))
        growsUpInto.add(GrowthEntry("Ram_Lamb"            , "Ram"           , 86400))
        growsUpInto.add(GrowthEntry("Skrill_Chick"        , "Skrill"        , 86400))
        growsUpInto.add(GrowthEntry("Turkey_Chick"        , "Turkey"        , 86400))
        growsUpInto.add(GrowthEntry("Warthog_Piglet"      , "Warthog"       , 86400))
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
            .append(
                KeyedCodec("GrowsUpInto", ListCodec()),
                { config, value -> config.growsUpInto = value },
                { it.growsUpInto }
            ).add()
            .build()!!
    }
}
