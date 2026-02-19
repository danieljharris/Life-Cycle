package DrDan.AnimalsBreed.config

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

class AnimalsBreedConfig {
    var breedGroup: MutableList<BreedEntry> = ArrayList()
    var babyForAdult: MutableList<BabyForAdultEntry> = ArrayList()

    constructor() {
        breedGroup.add(BreedEntry(arrayOf("Tamed_Sheep", "Tamed_Ram", "Tamed_Mouflon"))) // Real thing, "Mouflon" can breed with "Sheep" and produce fertile offspring
        breedGroup.add(BreedEntry(arrayOf("Tamed_Pig", "Tamed_Boar", "Tamed_Pig_Wild")))
        breedGroup.add(BreedEntry(arrayOf("Tamed_Camel")))
        breedGroup.add(BreedEntry(arrayOf("Tamed_Chicken_Desert", "Tamed_Chicken", "Tamed_Skrill")))
        breedGroup.add(BreedEntry(arrayOf("Tamed_Cow", "Tamed_Bison"))) // Real thing, called "Beefalo"
        breedGroup.add(BreedEntry(arrayOf("Tamed_Goat")))
        breedGroup.add(BreedEntry(arrayOf("Tamed_Horse")))
        breedGroup.add(BreedEntry(arrayOf("Tamed_Rabbit")))
        breedGroup.add(BreedEntry(arrayOf("Tamed_Turkey"))) // No real-world records of being able to breed with domestic chickens
        breedGroup.add(BreedEntry(arrayOf("Tamed_Warthog"))) // No recorded real-world records of being able to breed with domestic pigs/boars

        babyForAdult.add(BabyForAdultEntry("Tamed_Sheep"         , "Tamed_Sheep_Lamb"          ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Pig"           , "Tamed_Pig_Piglet"          ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Bison"         , "Tamed_Bison_Calf"          ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Boar"          , "Tamed_Boar_Piglet"         ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Camel"         , "Tamed_Camel_Calf"          ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Chicken_Desert", "Tamed_Chicken_Desert_Chick"))
        babyForAdult.add(BabyForAdultEntry("Tamed_Chicken"       , "Tamed_Chicken_Chick"       ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Cow"           , "Tamed_Cow_Calf"            ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Goat"          , "Tamed_Goat_Kid"            ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Horse"         , "Tamed_Horse_Foal"          ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Mouflon"       , "Tamed_Mouflon_Lamb"        ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Pig_Wild"      , "Tamed_Pig_Wild_Piglet"     ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Rabbit"        , "Tamed_Bunny"               ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Ram"           , "Tamed_Ram_Lamb"            ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Skrill"        , "Tamed_Skrill_Chick"        ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Turkey"        , "Tamed_Turkey_Chick"        ))
        babyForAdult.add(BabyForAdultEntry("Tamed_Warthog"       , "Tamed_Warthog_Piglet"      ))
    }

    private class ListCodec : Codec<MutableList<BreedEntry>> {
        private val arrayCodec = ArrayCodec(BreedEntry.CODEC) { size -> arrayOfNulls<BreedEntry>(size) }

        override fun decode(bsonValue: BsonValue, extraInfo: ExtraInfo): MutableList<BreedEntry>? {
            val arr = arrayCodec.decode(bsonValue, extraInfo)
            return if (arr != null) arr.toMutableList() else ArrayList()
        }

        override fun encode(list: MutableList<BreedEntry>, extraInfo: ExtraInfo): BsonValue {
            val arr = list.toTypedArray()
            return arrayCodec.encode(arr, extraInfo)
        }

        override fun decodeJson(reader: RawJsonReader, extraInfo: ExtraInfo): MutableList<BreedEntry>? {
            val arr = arrayCodec.decodeJson(reader, extraInfo)
            return if (arr != null) arr.toMutableList() else ArrayList()
        }

        override fun toSchema(context: SchemaContext): Schema {
            return arrayCodec.toSchema(context)
        }
    }

    companion object {
        @JvmStatic
        val CODEC = BuilderCodec.builder(AnimalsBreedConfig::class.java, ::AnimalsBreedConfig)
            .append(
                KeyedCodec("BreedGroup", ListCodec()),
                { config, value -> config.breedGroup = value },
                { it.breedGroup }
            ).add()
            .build()!!
    }
}
