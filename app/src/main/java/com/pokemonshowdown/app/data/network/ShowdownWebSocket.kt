package com.pokemonshowdown.app.data.network

import android.util.Log
import com.pokemonshowdown.app.data.model.ShowdownMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ShowdownWebSocket {

    companion object {
        private const val TAG = "ShowdownWS"
        private const val WS_URL = "wss://sim.smogon.com/showdown/websocket"
        private const val LOGIN_URL = "https://play.pokemonshowdown.com/action.php"
    }

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var challStr: String = ""
    private var challId: String = ""

    private val _messages = MutableSharedFlow<ShowdownMessage>(extraBufferCapacity = 100)
    val messages = _messages.asSharedFlow()

    private val _rawLogs = MutableSharedFlow<String>(extraBufferCapacity = 200)
    val rawLogs = _rawLogs.asSharedFlow()

    // ── Connexion ──────────────────────────────────────────────────────────────
    fun connect() {
        val request = Request.Builder().url(WS_URL).build()
        webSocket = client.newWebSocket(request, createListener())
        Log.d(TAG, "Connecting to Showdown...")
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    // ── Envoi de messages ──────────────────────────────────────────────────────
    fun send(message: String) {
        Log.d(TAG, "SEND: $message")
        webSocket?.send(message)
    }

    fun sendMove(roomId: String, moveIndex: Int) {
        send("$roomId|/choose move $moveIndex")
    }

    fun sendSwitch(roomId: String, pokemonIndex: Int) {
        send("$roomId|/choose switch $pokemonIndex")
    }

    fun searchBattle(format: String = "gen9randombattle") {
        send("|/search $format")
    }

    fun cancelSearch() {
        send("|/cancelsearch")
    }

    fun sendChat(roomId: String, message: String) {
        send("$roomId|$message")
    }

    fun forfeit(roomId: String) {
        send("$roomId|/forfeit")
    }

    // ── Authentification ───────────────────────────────────────────────────────
    suspend fun login(username: String, password: String): Boolean {
        return try {
            val assertion = getAssertion(username, password)
            if (assertion.isNotEmpty()) {
                send("|/trn $username,0,$assertion")
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}")
            false
        }
    }

    suspend fun loginGuest(username: String): Boolean {
        return try {
            val assertion = getGuestAssertion(username)
            send("|/trn $username,0,$assertion")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Guest login error: ${e.message}")
            false
        }
    }

    private suspend fun getAssertion(username: String, password: String): String {
        val httpClient = okhttp3.OkHttpClient()
        val body = okhttp3.FormBody.Builder()
            .add("act", "login")
            .add("name", username)
            .add("pass", password)
            .add("challstr", "$challId|$challStr")
            .build()

        val req = Request.Builder().url(LOGIN_URL).post(body).build()
        val response = httpClient.newCall(req).execute()
        val text = response.body?.string() ?: return ""

        // Response starts with ']' then JSON
        val json = JSONObject(text.trimStart(']'))
        return json.optString("assertion", "")
    }

    private suspend fun getGuestAssertion(username: String): String {
        val httpClient = okhttp3.OkHttpClient()
        val body = okhttp3.FormBody.Builder()
            .add("act", "getassertion")
            .add("userid", username.lowercase().replace(" ", ""))
            .add("challstr", "$challId|$challStr")
            .build()

        val req = Request.Builder().url(LOGIN_URL).post(body).build()
        val response = httpClient.newCall(req).execute()
        return response.body?.string()?.trim() ?: ""
    }

    // ── Listener WebSocket ─────────────────────────────────────────────────────
    private fun createListener() = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected!")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "RECV: $text")
            _rawLogs.tryEmit(text)
            parseMessage(text)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${t.message}")
            _messages.tryEmit(ShowdownMessage.Raw("ERROR: ${t.message}"))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $reason")
        }
    }

    // ── Parser de messages Showdown ────────────────────────────────────────────
    private fun parseMessage(raw: String) {
        val lines = raw.split("\n")
        var roomId = ""

        for (line in lines) {
            if (line.startsWith(">")) {
                roomId = line.substring(1).trim()
                continue
            }

            if (!line.startsWith("|")) continue
            val parts = line.split("|").drop(1) // remove leading empty
            if (parts.isEmpty()) continue

            val type = parts[0]
            when (type) {
                "challstr" -> {
                    challId = parts.getOrElse(1) { "" }
                    challStr = parts.getOrElse(2) { "" }
                    _messages.tryEmit(ShowdownMessage.ChallStr(challId, challStr))
                }
                "updateuser" -> {
                    val username = parts.getOrElse(1) { "" }.trimStart(' ', '\uFEFF')
                    val named = parts.getOrElse(2) { "0" } == "1"
                    _messages.tryEmit(ShowdownMessage.UpdateUser(username, named))
                }
                "init" -> {
                    if (roomId.startsWith("battle-")) {
                        _messages.tryEmit(ShowdownMessage.BattleStart(roomId, "", ""))
                    }
                }
                "request" -> {
                    val json = parts.getOrElse(1) { "" }
                    if (json.isNotEmpty()) {
                        _messages.tryEmit(ShowdownMessage.RequestAction(json))
                    }
                }
                "win" -> {
                    val winner = parts.getOrElse(1) { "" }
                    _messages.tryEmit(ShowdownMessage.Win(winner))
                }
                else -> {
                    _messages.tryEmit(ShowdownMessage.BattleLog(roomId, listOf(line)))
                }
            }
        }
    }
}
