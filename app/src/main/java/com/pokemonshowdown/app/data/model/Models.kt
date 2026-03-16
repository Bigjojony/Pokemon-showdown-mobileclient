package com.pokemonshowdown.app.data.model

// ── Pokémon en combat ──────────────────────────────────────────────────────────
data class BattlePokemon(
    val name: String,
    val species: String,
    val hp: Int,
    val maxHp: Int,
    val status: String? = null,          // brn, par, slp, frz, psn, tox
    val isActive: Boolean = false,
    val isFainted: Boolean = false,
    val moves: List<BattleMove> = emptyList(),
    val spriteUrl: String = ""
) {
    val hpPercent: Float get() = if (maxHp > 0) hp.toFloat() / maxHp else 0f
}

data class BattleMove(
    val id: String,
    val name: String,
    val pp: Int,
    val maxPp: Int,
    val type: String,
    val disabled: Boolean = false
)

// ── État du combat ─────────────────────────────────────────────────────────────
data class BattleState(
    val roomId: String = "",
    val playerUsername: String = "",
    val opponentUsername: String = "",
    val myTeam: List<BattlePokemon> = emptyList(),
    val opponentTeam: List<BattlePokemon> = emptyList(),
    val myActivePokemon: BattlePokemon? = null,
    val opponentActivePokemon: BattlePokemon? = null,
    val log: List<String> = emptyList(),
    val weather: String = "none",
    val turn: Int = 0,
    val isWaitingForAction: Boolean = false,
    val isMyTurn: Boolean = false,
    val winner: String? = null,
    val isOver: Boolean = false
)

// ── Messages réseau ────────────────────────────────────────────────────────────
sealed class ShowdownMessage {
    data class ChallStr(val id: String, val str: String) : ShowdownMessage()
    data class UpdateUser(val username: String, val named: Boolean) : ShowdownMessage()
    data class BattleStart(val roomId: String, val p1: String, val p2: String) : ShowdownMessage()
    data class BattleLog(val roomId: String, val lines: List<String>) : ShowdownMessage()
    data class RequestAction(val json: String) : ShowdownMessage()
    data class Win(val username: String) : ShowdownMessage()
    data class Raw(val raw: String) : ShowdownMessage()
    object Unknown : ShowdownMessage()
}

// ── État connexion ─────────────────────────────────────────────────────────────
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val username: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

// ── Type Pokémon (couleurs) ────────────────────────────────────────────────────
enum class PokemonType(val colorHex: String) {
    NORMAL("#A8A878"),
    FIRE("#F08030"),
    WATER("#6890F0"),
    ELECTRIC("#F8D030"),
    GRASS("#78C850"),
    ICE("#98D8D8"),
    FIGHTING("#C03028"),
    POISON("#A040A0"),
    GROUND("#E0C068"),
    FLYING("#A890F0"),
    PSYCHIC("#F85888"),
    BUG("#A8B820"),
    ROCK("#B8A038"),
    GHOST("#705898"),
    DRAGON("#7038F8"),
    DARK("#705848"),
    STEEL("#B8B8D0"),
    FAIRY("#EE99AC"),
    UNKNOWN("#68A090");

    companion object {
        fun fromString(type: String): PokemonType =
            values().find { it.name.equals(type, ignoreCase = true) } ?: UNKNOWN
    }
}
