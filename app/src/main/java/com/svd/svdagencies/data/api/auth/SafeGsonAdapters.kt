package com.svd.svdagencies.data.api.auth

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

class SafeIntAdapter : TypeAdapter<Int?>() {
    override fun write(out: JsonWriter, value: Int?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value)
        }
    }

    override fun read(reader: JsonReader): Int? {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            JsonToken.NUMBER -> {
                try {
                    reader.nextInt()
                } catch (e: Exception) {
                    reader.nextDouble().toInt()
                }
            }
            JsonToken.STRING -> {
                val str = reader.nextString().trim()
                str.toDoubleOrNull()?.toInt()
            }
            JsonToken.BOOLEAN -> {
                if (reader.nextBoolean()) 1 else 0
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }
}

class SafeDoubleAdapter : TypeAdapter<Double?>() {
    override fun write(out: JsonWriter, value: Double?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value)
        }
    }

    override fun read(reader: JsonReader): Double? {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            JsonToken.NUMBER -> {
                reader.nextDouble()
            }
            JsonToken.STRING -> {
                val str = reader.nextString().trim()
                str.toDoubleOrNull()
            }
            JsonToken.BOOLEAN -> {
                if (reader.nextBoolean()) 1.0 else 0.0
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }
}

class SafeBooleanAdapter : TypeAdapter<Boolean?>() {
    override fun write(out: JsonWriter, value: Boolean?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value)
        }
    }

    override fun read(reader: JsonReader): Boolean? {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            JsonToken.BOOLEAN -> {
                reader.nextBoolean()
            }
            JsonToken.NUMBER -> {
                reader.nextDouble() != 0.0
            }
            JsonToken.STRING -> {
                val str = reader.nextString().trim()
                str.equals("true", ignoreCase = true) || str == "1"
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }
}

fun createSafeGson(): Gson {
    return GsonBuilder()
        .registerTypeAdapter(Int::class.javaPrimitiveType, SafeIntAdapter())
        .registerTypeAdapter(Int::class.javaObjectType, SafeIntAdapter())
        .registerTypeAdapter(Double::class.javaPrimitiveType, SafeDoubleAdapter())
        .registerTypeAdapter(Double::class.javaObjectType, SafeDoubleAdapter())
        .registerTypeAdapter(Boolean::class.javaPrimitiveType, SafeBooleanAdapter())
        .registerTypeAdapter(Boolean::class.javaObjectType, SafeBooleanAdapter())
        .create()
}
