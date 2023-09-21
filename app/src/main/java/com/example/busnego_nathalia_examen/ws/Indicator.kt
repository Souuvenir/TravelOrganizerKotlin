package com.example.busnego_nathalia_examen.ws

import com.squareup.moshi.Json


data class IndicatorResult(
    val serie:Array<Indicator>
)
data class Indicator(
    @Json(name = "fecha")
    val date:String,
    @Json(name = "valor")
    val valor:String
) {
}