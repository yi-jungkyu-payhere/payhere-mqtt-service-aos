package com.payhere.retrofit

import com.payhere.mqtt.ReqMonitorStatus
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Url

interface RetrofitService {
    @PATCH("/api/v1/sellers/{sid}/devices/{app_identifier}")
    fun patchMonitoring(
        @Header("Authorization") accessToken: String,
        @Path("sid") sid: String,
        @Path("app_identifier") appIdentifier: String,
        @Body data: ReqMonitorStatus,
    ): Call<Response<String>>
}
