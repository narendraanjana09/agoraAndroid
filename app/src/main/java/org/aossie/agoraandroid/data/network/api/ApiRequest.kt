package org.aossie.agoraandroid.data.network.api

import org.aossie.agoraandroid.utilities.ApiException
import org.aossie.agoraandroid.utilities.AppConstants
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response

abstract class ApiRequest {
  suspend fun <T : Any> apiRequest(call: suspend () -> Response<T>): T {

    val response = call.invoke()
    if (response.isSuccessful) {
      return response.body()!!
    } else {
      val error = response.errorBody().toString()
      val message = StringBuilder()
      error.let {
        try {
          message.append(JSONObject(it).getString("message"))
        } catch (e: JSONException) {
        }
        if (message.isNotEmpty()) message.append("\n")
      }
      when (response.code()) {
        AppConstants.BAD_REQUEST_CODE -> message.append(AppConstants.BAD_REQUEST_MESSAGE)
        AppConstants.UNAUTHENTICATED_CODE -> message.append(AppConstants.UNAUTHENTICATED_MESSAGE)
        AppConstants.INVALID_CREDENTIALS_CODE -> message.append(AppConstants.INVALID_CREDENTIALS_MESSAGE)
        AppConstants.NOT_FOUND_CODE -> message.append(AppConstants.NOT_FOUND_MESSAGE)
        AppConstants.INTERNAL_SERVER_ERROR_CODE -> message.append(AppConstants.INTERNAL_SERVER_ERROR_MESSAGE)
      }
      if (message.isEmpty()) message.append(response.code().toString())
      throw ApiException(message.toString())
    }
  }
}
