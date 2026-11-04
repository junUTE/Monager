package com.monager.app.data.local.entity

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromTxType(v: TxType): String = v.name
    @TypeConverter fun toTxType(v: String): TxType = TxType.valueOf(v)
}
