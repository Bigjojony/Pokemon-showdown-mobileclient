package com.pokemonshowdown.app.data.repository

import com.pokemonshowdown.app.data.model.*
import com.pokemonshowdown.app.data.network.ShowdownWebSocket
import kotlinx.coroutines.flow.*
import org.json.JSONObject

class ShowdownRepository(private val ws: ShowdownWebSocket = ShowdownWebSocket()) {

    val messages = ws.messages
    val rawLogs = ws.rawLogs

    // ── Parse le JSON de request pour extraire les moves/pokémon ──────────────
    fun parseRequest(json: String): Pair<List<BattleMove>, List<BattlePokemon>> {
        val moves = mutableListOf<BattleMove>()
        val team = mutableListOf<BattlePokemon>()

        try {
            val obj = JSONObject(json)

            // Moves de l'actif
            if (obj.has("active")) {
                val activeArr = obj.getJSONArray("active")
                if (activeArr.length() > 0) {
                    val active = activeArr.getJSONObject(0)
                    val movesArr = active.getJSONArray("moves")
                    for (i in 0 until movesArr.length()) {
                        val m = movesArr.getJSONObject(i)
                        moves.add(
                            BattleMove(
                                id = m.optString("id"),
                                name = m.optString("move"),
                                pp = m.optInt("pp", 0),
                                maxPp = m.optInt("maxpp", 0),
                                type = m.optString("type", "Normal"),
                                disabled = m.optBoolean("disabled", false)
                            )
                        )
                    }
                }
            }

            // Équipe
            if (obj.has("side")) {
                val side = obj.getJSONObject("side")
                val pokemon = side.getJSONArray("pokemon")
                for (i in 0 until pokemon.length()) {
                    val p = pokemon.getJSONObject(i)
                    val hpStr = p.optString("condition", "100/100")
                    val (hp, maxHp) = parseHp(hpStr)
                    val details = p.optString("details", "")
                    val species = details.split(",")[0].trim()

                    team.add(
                        BattlePokemon(
                            name = species,
                            species = species.lowercase().replace(" ", "").replace("-", ""),
                            hp = hp,
                            maxHp = maxHp,
                            status = extractStatus(hpStr),
                            isActive = p.optBoolean("active", false),
                            isFainted = hp == 0,
                            spriteUrl = buildSpriteUrl(species)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(moves, team)
    }

    private fun parseHp(condition: String): Pair<Int, Int> {
        return try {
            val hpPart = condition.split(" ")[0]
            val parts = hpPart.split("/")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            Pair(100, 100)
        }
    }

    private fun extractStatus(condition: String): String? {
        val parts = condition.split(" ")
        return if (parts.size > 1) parts[1] else null
    }

    fun buildSpriteUrl(species: String): String {
        val clean = species.lowercase()
            .replace(" ", "")
            .replace("-", "")
            .replace(".", "")
        return "https://play.pokemonshowdown.com/sprites/gen5/$clean.png"
    }

    // ── Délégation WebSocket ───────────────────────────────────────────────────
    fun connect() = ws.connect()
    fun disconnect() = ws.disconnect()
    suspend fun login(u: String, p: String) = ws.login(u, p)
    suspend fun loginGuest(u: String) = ws.loginGuest(u)
    fun searchBattle(format: String) = ws.searchBattle(format)
    fun cancelSearch() = ws.cancelSearch()
    fun sendMove(roomId: String, index: Int) = ws.sendMove(roomId, index)
    fun sendSwitch(roomId: String, index: Int) = ws.sendSwitch(roomId, index)
    fun sendChat(roomId: String, msg: String) = ws.sendChat(roomId, msg)
    fun forfeit(roomId: String) = ws.forfeit(roomId)
}
