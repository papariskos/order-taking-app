package com.example.restaurantordering.data.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebSocketService {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Disable timeout for websockets
        .build()

    private var webSocket: WebSocket? = null
    private val gson = Gson()

    private val _events = MutableSharedFlow<WebSocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WebSocketEvent> = _events

    fun connect(token: String, serverIp: String = "10.0.2.2:3000") {
        disconnect()

        val request = Request.Builder()
            .url("ws://$serverIp?token=$token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocketService", "Connected to WebSocket server")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocketService", "Received message: $text")
                try {
                    val jsonObject = gson.fromJson(text, JsonObject::class.java)
                    if (jsonObject.has("event")) {
                        val event = jsonObject.get("event").asString
                        val data = jsonObject.get("data")
                        
                        _events.tryEmit(WebSocketEvent(event, data.toString()))
                    }
                } catch (e: Exception) {
                    Log.e("WebSocketService", "Error parsing WebSocket message", e)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketService", "WebSocket closed: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketService", "WebSocket failure", t)
                // Simple auto-reconnect trigger after 5 seconds
                Thread {
                    Thread.sleep(5000)
                    Log.d("WebSocketService", "Attempting WebSocket reconnect...")
                    connect(token, serverIp)
                }.start()
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Goodbye")
        webSocket = null
    }
}

data class WebSocketEvent(
    val event: String,
    val dataJson: String
)
