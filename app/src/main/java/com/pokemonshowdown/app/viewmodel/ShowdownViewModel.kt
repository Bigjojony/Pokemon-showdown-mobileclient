package com.pokemonshowdown.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokemonshowdown.app.data.model.*
import com.pokemonshowdown.app.data.repository.ShowdownRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ShowdownViewModel : ViewModel() {

    private val repo = ShowdownRepository()

    // ── États UI ───────────────────────────────────────────────────────────────
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _battleState = MutableStateFlow(BattleState())
    val battleState: StateFlow<BattleState> = _battleState

    private val _currentMoves = MutableStateFlow<List<BattleMove>>(emptyList())
    val currentMoves: StateFlow<List<BattleMove>> = _currentMoves

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _chatMessages = MutableStateFlow<List<String>>(emptyList())
    val chatMessages: StateFlow<List<String>> = _chatMessages

    private val _isInBattle = MutableStateFlow(false)
    val isInBattle: StateFlow<Boolean> = _isInBattle

    private val _currentRoomId = MutableStateFlow("")
    val currentRoomId: StateFlow<String> = _currentRoomId

    private val _selectedFormat = MutableStateFlow("gen9randombattle")
    val selectedFormat: StateFlow<String> = _selectedFormat

    init {
        observeMessages()
    }

    // ── Connexion ──────────────────────────────────────────────────────────────
    fun connect() {
        _connectionState.value = ConnectionState.Connecting
        repo.connect()
    }

    fun disconnect() {
        repo.disconnect()
        _connectionState.value = ConnectionState.Disconnected
        _isInBattle.value = false
        _battleState.value = BattleState()
    }

    fun loginWithAccount(username: String, password: String) {
        viewModelScope.launch {
            val success = repo.login(username, password)
            if (!success) {
                _connectionState.value = ConnectionState.Error("Identifiants incorrects")
            }
        }
    }

    fun loginAsGuest(username: String) {
        viewModelScope.launch {
            repo.loginGuest(username)
        }
    }

    // ── Matchmaking ────────────────────────────────────────────────────────────
    fun searchBattle() {
        _isSearching.value = true
        repo.searchBattle(_selectedFormat.value)
    }

    fun cancelSearch() {
        _isSearching.value = false
        repo.cancelSearch()
    }

    fun setFormat(format: String) {
        _selectedFormat.value = format
    }

    // ── Actions de combat ──────────────────────────────────────────────────────
    fun useMove(moveIndex: Int) {
        val roomId = _currentRoomId.value
        if (roomId.isNotEmpty()) {
            repo.sendMove(roomId, moveIndex + 1) // Showdown index commence à 1
            _battleState.update { it.copy(isWaitingForAction = false, isMyTurn = false) }
        }
    }

    fun switchPokemon(pokemonIndex: Int) {
        val roomId = _currentRoomId.value
        if (roomId.isNotEmpty()) {
            repo.sendSwitch(roomId, pokemonIndex + 1)
            _battleState.update { it.copy(isWaitingForAction = false, isMyTurn = false) }
        }
    }

    fun forfeit() {
        val roomId = _currentRoomId.value
        if (roomId.isNotEmpty()) {
            repo.forfeit(roomId)
        }
    }

    fun sendChat(message: String) {
        val roomId = _currentRoomId.value
        if (roomId.isNotEmpty() && message.isNotBlank()) {
            repo.sendChat(roomId, message)
        }
    }

    // ── Observer messages ──────────────────────────────────────────────────────
    private fun observeMessages() {
        viewModelScope.launch {
            repo.messages.collect { message ->
                handleMessage(message)
            }
        }

        viewModelScope.launch {
            repo.rawLogs.collect { raw ->
                parseBattleLogs(raw)
            }
        }
    }

    private fun handleMessage(message: ShowdownMessage) {
        when (message) {
            is ShowdownMessage.ChallStr -> {
                // Prêt à se connecter
            }
            is ShowdownMessage.UpdateUser -> {
                if (message.named) {
                    _connectionState.value = ConnectionState.Connected(message.username)
                    _battleState.update { it.copy(playerUsername = message.username) }
                }
            }
            is ShowdownMessage.BattleStart -> {
                _isSearching.value = false
                _isInBattle.value = true
                _currentRoomId.value = message.roomId
                _battleState.update {
                    it.copy(roomId = message.roomId)
                }
            }
            is ShowdownMessage.RequestAction -> {
                val (moves, team) = repo.parseRequest(message.json)
                _currentMoves.value = moves
                _battleState.update { state ->
                    state.copy(
                        myTeam = team,
                        myActivePokemon = team.firstOrNull { it.isActive },
                        isMyTurn = true,
                        isWaitingForAction = true
                    )
                }
            }
            is ShowdownMessage.Win -> {
                val myUsername = (_connectionState.value as? ConnectionState.Connected)?.username ?: ""
                _battleState.update {
                    it.copy(
                        winner = message.username,
                        isOver = true,
                        isMyTurn = false
                    )
                }
                _isInBattle.value = false
            }
            else -> {}
        }
    }

    private fun parseBattleLogs(raw: String) {
        val lines = raw.split("\n")
        val newLogs = mutableListOf<String>()

        for (line in lines) {
            when {
                line.startsWith("|move|") -> {
                    val parts = line.split("|")
                    val pokemon = parts.getOrElse(2) { "" }.split(":").last().trim()
                    val move = parts.getOrElse(3) { "" }
                    newLogs.add("⚔️ $pokemon utilise $move !")
                }
                line.startsWith("|-damage|") -> {
                    val parts = line.split("|")
                    val pokemon = parts.getOrElse(2) { "" }.split(":").last().trim()
                    val hp = parts.getOrElse(3) { "" }
                    newLogs.add("💥 $pokemon : $hp PV")
                }
                line.startsWith("|-heal|") -> {
                    val parts = line.split("|")
                    val pokemon = parts.getOrElse(2) { "" }.split(":").last().trim()
                    newLogs.add("💚 $pokemon se soigne !")
                }
                line.startsWith("|switch|") -> {
                    val parts = line.split("|")
                    val pokemon = parts.getOrElse(3) { "" }.split(",")[0]
                    val player = parts.getOrElse(2) { "" }
                    val who = if (player.startsWith("p1")) "Tu" else "Adversaire"
                    newLogs.add("🔄 $who envoie $pokemon !")

                    // Mettre à jour le pokémon adverse si p2
                    if (!player.startsWith("p1")) {
                        _battleState.update { state ->
                            state.copy(
                                opponentActivePokemon = BattlePokemon(
                                    name = pokemon,
                                    species = pokemon.lowercase().replace(" ", ""),
                                    hp = 100, maxHp = 100,
                                    spriteUrl = repo.buildSpriteUrl(pokemon)
                                )
                            )
                        }
                    }
                }
                line.startsWith("|faint|") -> {
                    val parts = line.split("|")
                    val pokemon = parts.getOrElse(2) { "" }.split(":").last().trim()
                    newLogs.add("💀 $pokemon est K.O. !")
                }
                line.startsWith("|-weather|") -> {
                    val parts = line.split("|")
                    val weather = parts.getOrElse(2) { "none" }
                    newLogs.add("🌦️ Météo : $weather")
                    _battleState.update { it.copy(weather = weather) }
                }
                line.startsWith("|turn|") -> {
                    val parts = line.split("|")
                    val turn = parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0
                    newLogs.add("── Tour $turn ──")
                    _battleState.update { it.copy(turn = turn) }
                }
                line.startsWith("|-status|") -> {
                    val parts = line.split("|")
                    val pokemon = parts.getOrElse(2) { "" }.split(":").last().trim()
                    val status = parts.getOrElse(3) { "" }
                    newLogs.add("🔴 $pokemon est affecté : $status")
                }
                line.startsWith("|player|") -> {
                    val parts = line.split("|")
                    val slot = parts.getOrElse(2) { "" }
                    val name = parts.getOrElse(3) { "" }
                    val myUser = (_connectionState.value as? ConnectionState.Connected)?.username ?: ""
                    if (slot == "p2" && name != myUser) {
                        _battleState.update { it.copy(opponentUsername = name) }
                    }
                }
            }
        }

        if (newLogs.isNotEmpty()) {
            _battleState.update { state ->
                state.copy(log = (state.log + newLogs).takeLast(100))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repo.disconnect()
    }
}
