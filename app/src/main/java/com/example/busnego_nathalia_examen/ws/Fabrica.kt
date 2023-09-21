package com.example.busnego_nathalia_examen.ws

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object Fabrica {

    fun getBaseUrl():String = "https://mindicador.cl"

    fun getService( serviceClass: Class<*>):Any{
        val adapter = KotlinJsonAdapterFactory()
        val moshi = Moshi.Builder().add(adapter).build()
        val converter = MoshiConverterFactory.create(moshi)
        val retrofit = Retrofit.Builder()
            .addConverterFactory(converter)
            .baseUrl(getBaseUrl())
            .build()
        return retrofit.create(serviceClass)
    }
    fun getDailyIndicator():IndicatorService{
        return getService(IndicatorService::class.java) as IndicatorService
    }
}