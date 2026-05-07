package io.github.androidpoet.kdb.query

import io.github.androidpoet.kdb.core.SqlValue
import io.github.androidpoet.kdb.driver.KdbCursor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
public class AutoTable<R : Any>(
    override val tableName: String,
    private val serializer: KSerializer<R>
) : Table<R> {

    override val columns: List<Column<*>> = (0 until serializer.descriptor.elementsCount).map { i ->
        val name = serializer.descriptor.getElementName(i)
        val kind = serializer.descriptor.getElementDescriptor(i).kind
        val type = when (kind) {
            PrimitiveKind.LONG, PrimitiveKind.INT, PrimitiveKind.SHORT, PrimitiveKind.BYTE -> ColumnType.LONG
            PrimitiveKind.DOUBLE, PrimitiveKind.FLOAT -> ColumnType.DOUBLE
            PrimitiveKind.BOOLEAN -> ColumnType.BOOLEAN
            else -> ColumnType.TEXT
        }
        Column<Any>(name, type)
    }

    override fun fromRow(cursor: KdbCursor): R {
        val decoder = KdbDecoder(cursor, serializer.descriptor)
        return serializer.deserialize(decoder)
    }

    override fun toValues(row: R): Map<String, SqlValue> {
        val encoder = KdbEncoder(serializer.descriptor)
        serializer.serialize(encoder, row)
        return encoder.values
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class KdbEncoder(private val descriptor: SerialDescriptor) : AbstractEncoder() {
    val values = mutableMapOf<String, SqlValue>()
    private var currentIndex = -1

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        currentIndex = index
        return true
    }

    private fun put(value: SqlValue) {
        values[descriptor.getElementName(currentIndex)] = value
    }

    override fun encodeLong(value: Long) = put(SqlValue.LongValue(value))
    override fun encodeInt(value: Int) = put(SqlValue.LongValue(value.toLong()))
    override fun encodeString(value: String) = put(SqlValue.TextValue(value))
    override fun encodeBoolean(value: Boolean) = put(SqlValue.BooleanValue(value))
    override fun encodeDouble(value: Double) = put(SqlValue.DoubleValue(value))
    override fun encodeFloat(value: Float) = put(SqlValue.DoubleValue(value.toDouble()))
    override fun encodeNull() = put(SqlValue.Null)
}

@OptIn(ExperimentalSerializationApi::class)
private class KdbDecoder(
    private val cursor: KdbCursor,
    private val descriptor: SerialDescriptor
) : AbstractDecoder() {
    private var currentIndex = -1

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        currentIndex++
        return if (currentIndex < descriptor.elementsCount) currentIndex else CompositeDecoder.DECODE_DONE
    }

    override fun decodeLong(): Long = cursor.getLong(currentIndex)
    override fun decodeInt(): Int = cursor.getLong(currentIndex).toInt()
    override fun decodeString(): String = cursor.getText(currentIndex)
    override fun decodeBoolean(): Boolean = cursor.getLong(currentIndex) == 1L
    override fun decodeDouble(): Double = cursor.getDouble(currentIndex)
    override fun decodeFloat(): Float = cursor.getDouble(currentIndex).toFloat()
    override fun decodeNull(): Nothing? = null
    
    override fun decodeNotNullMark(): Boolean = !cursor.isNull(currentIndex)
}

// For toValues, we can use a simpler approach for now:
// Since we want "Crazy Simple", I'll add a helper that uses the serializer to encode to a Map.
