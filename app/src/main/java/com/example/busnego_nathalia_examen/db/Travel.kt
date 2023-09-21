package com.example.busnego_nathalia_examen.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Travel(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    var nombre: String,
    var latitud: Int,
    var longitud: Int,
    var imagen:String ,
    var orden: Int,
    val alojamiento: Int,
    val traslado: Int,
    val comentario: String
)
