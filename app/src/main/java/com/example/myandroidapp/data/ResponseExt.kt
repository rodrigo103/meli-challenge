package com.example.myandroidapp.data

import okhttp3.ResponseBody
import java.io.IOException

fun <T> retrofit2.Response<T>.extractBody(): T {
    if (isSuccessful) return body()!!
    val errorBody = errorBody()?.takeIf { it.contentLength() > 0 }
    val message = buildErrorMessage(errorBody)
    throw when (code()) {
        400 -> ApiException.BadRequest(message)
        401 -> ApiException.Unauthorized(message)
        404 -> ApiException.NotFound(message)
        409 -> ApiException.Conflict(message)
        in 500..599 -> ApiException.ServerError(code(), message)
        else -> IOException(message)
    }
}

private fun buildErrorMessage(errorBody: ResponseBody?): String {
    val details = errorBody?.let {
        try { it.string() } catch (e: IOException) { null }
    } ?: ""
    return if (details.isNotBlank()) details else "Unknown error"
}
