package com.xfyc.music.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET
    suspend fun get(@Url url: String, @HeaderMap headers: Map<String, String> = emptyMap()): Response<ResponseBody>

    @POST
    suspend fun post(
        @Url url: String,
        @Body body: okhttp3.RequestBody,
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    @GET
    @Streaming
    suspend fun download(@Url url: String, @HeaderMap headers: Map<String, String> = emptyMap()): Response<ResponseBody>
}
