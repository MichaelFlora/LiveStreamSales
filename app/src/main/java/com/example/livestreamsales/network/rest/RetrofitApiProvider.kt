package com.example.livestreamsales.network.rest

import com.example.livestreamsales.network.rest.api.base.IApi
import retrofit2.Retrofit
import javax.inject.Inject

class RetrofitApiProvider @Inject constructor(
    private val retrofit: Retrofit,
): IApiProvider {
    override fun <T: IApi> createApi(apiClass: Class<T>): T{
        return retrofit.create(apiClass)
    }
}