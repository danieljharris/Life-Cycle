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
        breedGroup.add(BreedEntry(arrayOf("Sheep", "Ram", "Mouflon"))) // Real thing, "Mouflon" can breed with "Sheep" and produce fertile offspring
        breedGroup.add(BreedEntry(arrayOf("Pig", "Boar", "Pig_Wild")))
        breedGroup.add(BreedEntry(arrayOf("Camel")))
        breedGroup.add(BreedEntry(arrayOf("Chicken_Desert", "Chicken", "Skrill")))
        breedGroup.add(BreedEntry(arrayOf("Cow", "Bison"))) // Real thing, called "Beefalo"
        breedGroup.add(BreedEntry(arrayOf("Goat")))
        breedGroup.add(BreedEntry(arrayOf("Horse")))
        breedGroup.add(BreedEntry(arrayOf("Rabbit")))
        breedGroup.add(BreedEntry(arrayOf("Turkey"))) // No real-world records of being able to breed with domestic chickens
        breedGroup.add(BreedEntry(arrayOf("Warthog"))) // No recorded real-world records of being able to breed with domestic pigs/boars

        babyForAdult.add(BabyForAdultEntry("Sheep"         , "Sheep_Lamb"          ))
        babyForAdult.add(BabyForAdultEntry("Pig"           , "Pig_Piglet"          ))
        babyForAdult.add(BabyForAdultEntry("Bison"         , "Bison_Calf"          ))
        babyForAdult.add(BabyForAdultEntry("Boar"          , "Boar_Piglet"         ))
        babyForAdult.add(BabyForAdultEntry("Camel"         , "Camel_Calf"          ))
        babyForAdult.add(BabyForAdultEntry("Chicken_Desert", "Chicken_Desert_Chick"))
        babyForAdult.add(BabyForAdultEntry("Chicken"       , "Chicken_Chick"       ))
        babyForAdult.add(BabyForAdultEntry("Cow"           , "Cow_Calf"            ))
        babyForAdult.add(BabyForAdultEntry("Goat"          , "Goat_Kid"            ))
        babyForAdult.add(BabyForAdultEntry("Horse"         , "Horse_Foal"          ))
        babyForAdult.add(BabyForAdultEntry("Mouflon"       , "Mouflon_Lamb"        ))
        babyForAdult.add(BabyForAdultEntry("Pig_Wild"      , "Pig_Wild_Piglet"     ))
        babyForAdult.add(BabyForAdultEntry("Rabbit"        , "Bunny"               ))
        babyForAdult.add(BabyForAdultEntry("Ram"           , "Ram_Lamb"            ))
        babyForAdult.add(BabyForAdultEntry("Skrill"        , "Skrill_Chick"        ))
        babyForAdult.add(BabyForAdultEntry("Turkey"        , "Turkey_Chick"        ))
        babyForAdult.add(BabyForAdultEntry("Warthog"       , "Warthog_Piglet"      ))
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
