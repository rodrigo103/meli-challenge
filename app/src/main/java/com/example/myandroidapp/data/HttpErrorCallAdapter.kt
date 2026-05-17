package com.example.myandroidapp.data

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class HttpErrorCallAdapterFactory : CallAdapter.Factory() {

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) return null
        val callType = getParameterUpperBound(0, returnType as ParameterizedType)
        if (getRawType(callType) != Response::class.java) return null
        val bodyType = getParameterUpperBound(0, callType as ParameterizedType)
        return HttpErrorCallAdapter<Any>(bodyType)
    }
}

private class HttpErrorCallAdapter<T>(
    private val bodyType: Type,
) : CallAdapter<T, Call<T>> {

    override fun responseType(): Type = bodyType

    override fun adapt(call: Call<T>): Call<T> {
        return HttpErrorCall(call)
    }
}

private class HttpErrorCall<T>(
    private val delegate: Call<T>,
) : Call<T> by delegate {

    override fun execute(): Response<T> {
        val response = delegate.execute()
        checkResponse(response)
        return response
    }

    override fun clone(): Call<T> = HttpErrorCall(delegate.clone())

    private fun checkResponse(response: Response<T>) {
        if (response.isSuccessful) return
        val errorBody = response.errorBody()?.takeIf { it.contentLength() > 0 }
        val message = buildErrorMessage(response, errorBody)
        throw when (response.code()) {
            400 -> ApiException.BadRequest(message)
            401 -> ApiException.Unauthorized(message)
            404 -> ApiException.NotFound(message)
            409 -> ApiException.Conflict(message)
            in 500..599 -> ApiException.ServerError(response.code(), message)
            else -> IOException(message)
        }
    }

    private fun buildErrorMessage(response: Response<*>, errorBody: ResponseBody?): String {
        val details = errorBody?.let {
            try { it.string() } catch (e: IOException) { null }
        } ?: response.message()
        return "HTTP ${response.code()}: $details"
    }
}
