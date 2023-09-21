package com.example.busnego_nathalia_examen.ws

import retrofit2.http.GET

interface IndicatorService {

    @GET("/api/dolar")
    suspend fun getMonthlyValues(): IndicatorResult

}