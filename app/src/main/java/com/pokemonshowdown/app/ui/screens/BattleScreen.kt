package com.pokemonshowdown.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pokemonshowdown.app.data.model.*
import com.pokemonshowdown.app.ui.theme.*

@Composable
fun BattleScreen(
    battleState: BattleState,
    currentMoves: List<BattleMove>,
    onUseMove: (Int) -> Unit,
    onSwitch: (Int) -> Unit,
    onForfeit: () -> Unit,
    onSendChat: (String) -> Unit,
    onBackToLobby: () -> Unit
) {
    var showSwitchPanel by remember { mutableStateOf(false) }
    var showChatPanel by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    val logState = rememberLazyListState()

    // Auto-scroll log
    LaunchedEffect(battleState.log.size) {
        if (battleState.log.isNotEmpty()) {
            logState.animateScrollToItem(battleState.log.size - 1)
        }
    }

    // Écran de victoire/défaite
    if (battleState.isOver) {
        BattleResultScreen(
            winner = battleState.winner ?: "",
            myUsername = battleState.playerUsername,
            onBackToLobby = onBackToLobby
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1B2838))))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────────────────
            BattleHeader(
                myUsername = battleState.playerUsername,
                opponentUsername = battleState.opponentUsername,
                turn = battleState.turn,
                weather = battleState.weather,
                onForfeit = onForfeit
            )

            // ── Terrain de combat ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1a3a5c), Color(0xFF2d5a27))
                        )
                    )
            ) {
                // Pokémon adverse (haut gauche)
                battleState.opponentActivePokemon?.let { pokemon ->
                    PokemonOnField(
                        pokemon = pokemon,
                        isOpponent = true,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }

                // Pokémon allié (bas droite)
                battleState.myActivePokemon?.let { pokemon ->
                    PokemonOnField(
                        pokemon = pokemon,
                        isOpponent = false,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }

                // Météo
                if (battleState.weather != "none" && battleState.weather.isNotEmpty()) {
                    WeatherIndicator(
                        weather = battleState.weather,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }

            // ── Barres de vie ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PokeGray)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                battleState.opponentActivePokemon?.let {
                    HpBar(pokemon = it, label = "Adverse: ${it.name}")
                }
                battleState.myActivePokemon?.let {
                    HpBar(pokemon = it, label = "Mon ${it.name}")
                }
            }

            // ── Log de combat ────────────────────────────────────────────────────
            LazyColumn(
                state = logState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.2f)
                    .background(Color(0xFF0A0A0A))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(battleState.log) { line ->
                    Text(
                        text = line,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }

            // ── Panneau d'actions ────────────────────────────────────────────────
            AnimatedContent(targetState = showSwitchPanel) { switching ->
                if (switching) {
                    SwitchPanel(
                        team = battleState.myTeam,
                        onSwitch = { index ->
                            onSwitch(index)
                            showSwitchPanel = false
                        },
                        onBack = { showSwitchPanel = false }
                    )
                } else {
                    MovePanel(
                        moves = currentMoves,
                        isMyTurn = battleState.isMyTurn,
                        onUseMove = onUseMove,
                        onShowSwitch = { showSwitchPanel = true },
                        onShowChat = { showChatPanel = !showChatPanel }
                    )
                }
            }

            // ── Chat ─────────────────────────────────────────────────────────────
            AnimatedVisibility(visible = showChatPanel) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PokeGrayLight)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Message...", color = Color.White.copy(alpha = 0.4f)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PokeYellow,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank()) {
                                onSendChat(chatInput)
                                chatInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Envoyer", tint = PokeYellow)
                    }
                }
            }
        }
    }
}

// ── Composants ─────────────────────────────────────────────────────────────────

@Composable
fun BattleHeader(
    myUsername: String,
    opponentUsername: String,
    turn: Int,
    weather: String,
    onForfeit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PokeGray)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(myUsername, color = PokeYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (turn > 0) Text("Tour $turn", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        Text("⚔️ VS ⚔️", color = PokeRed, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(opponentUsername.ifEmpty { "???" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onForfeit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Flag, contentDescription = "Abandonner", tint = PokeRed, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun PokemonOnField(pokemon: BattlePokemon, isOpponent: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = if (isOpponent) Alignment.Start else Alignment.End
    ) {
        val spriteUrl = if (isOpponent)
            "https://play.pokemonshowdown.com/sprites/gen5/${pokemon.species}.png"
        else
            "https://play.pokemonshowdown.com/sprites/gen5-back/${pokemon.species}.png"

        AsyncImage(
            model = spriteUrl,
            contentDescription = pokemon.name,
            modifier = Modifier.size(if (isOpponent) 90.dp else 110.dp),
            contentScale = ContentScale.Fit
        )

        // Status badge
        pokemon.status?.let { status ->
            StatusBadge(status = status)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, label) = when (status.lowercase()) {
        "brn" -> Pair(Color(0xFFFF6600), "BRÛ")
        "par" -> Pair(Color(0xFFFFFF00), "PAR")
        "slp" -> Pair(Color(0xFF9966FF), "SOM")
        "frz" -> Pair(Color(0xFF66CCFF), "GEL")
        "psn", "tox" -> Pair(Color(0xFFCC33CC), "PSN")
        else -> Pair(Color.Gray, status.uppercase())
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HpBar(pokemon: BattlePokemon, label: String) {
    val hpColor = when {
        pokemon.hpPercent > 0.5f -> HpGreen
        pokemon.hpPercent > 0.2f -> HpYellow
        else -> HpRed
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
            Text(
                "${pokemon.hp}/${pokemon.maxHp}",
                color = hpColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pokemon.hpPercent.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(hpColor)
            )
        }
    }
}

@Composable
fun WeatherIndicator(weather: String, modifier: Modifier = Modifier) {
    val (emoji, label) = when (weather.lowercase()) {
        "sunnyday", "desolateland" -> "☀️" to "Soleil"
        "raindance", "primordialsea" -> "🌧️" to "Pluie"
        "sandstorm" -> "🌪️" to "Sable"
        "hail", "snow" -> "❄️" to "Grêle"
        else -> "🌡️" to weather
    }
    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("$emoji $label", color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
fun MovePanel(
    moves: List<BattleMove>,
    isMyTurn: Boolean,
    onUseMove: (Int) -> Unit,
    onShowSwitch: () -> Unit,
    onShowChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PokeGray)
            .padding(12.dp)
    ) {
        if (!isMyTurn) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = PokeYellow,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adversaire réfléchit...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }
        }

        if (moves.isNotEmpty() && isMyTurn) {
            Text(
                "Choisissez une attaque",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // Grille 2x2
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                moves.chunked(2).forEachIndexed { rowIndex, rowMoves ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMoves.forEachIndexed { colIndex, move ->
                            val index = rowIndex * 2 + colIndex
                            MoveButton(
                                move = move,
                                onClick = { if (!move.disabled) onUseMove(index) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Remplir si nombre impair
                        if (rowMoves.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Actions secondaires
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onShowSwitch,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Switch", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onShowChat,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Chat", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun MoveButton(move: BattleMove, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val typeColor = try {
        Color(android.graphics.Color.parseColor(PokemonType.fromString(move.type).colorHex))
    } catch (e: Exception) {
        Color.Gray
    }

    Button(
        onClick = onClick,
        enabled = !move.disabled,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = typeColor.copy(alpha = 0.85f),
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                move.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                "${move.pp}/${move.maxPp} PP • ${move.type}",
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SwitchPanel(team: List<BattlePokemon>, onSwitch: (Int) -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PokeGray)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choisir un Pokémon", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        team.forEachIndexed { index, pokemon ->
            if (!pokemon.isActive) {
                PokemonSwitchRow(pokemon = pokemon, index = index, onSwitch = onSwitch)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun PokemonSwitchRow(pokemon: BattlePokemon, index: Int, onSwitch: (Int) -> Unit) {
    val hpColor = when {
        pokemon.hpPercent > 0.5f -> HpGreen
        pokemon.hpPercent > 0.2f -> HpYellow
        else -> HpRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PokeGrayLight)
            .clickable(enabled = !pokemon.isFainted) { onSwitch(index) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://play.pokemonshowdown.com/sprites/gen5/${pokemon.species}.png",
            contentDescription = pokemon.name,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                pokemon.name,
                color = if (pokemon.isFainted) Color.Gray else Color.White,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pokemon.hpPercent)
                        .fillMaxHeight()
                        .background(if (pokemon.isFainted) Color.Gray else hpColor)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (pokemon.isFainted) {
            Text("K.O.", color = HpRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        } else {
            Text("${pokemon.hp}/${pokemon.maxHp}", color = hpColor, fontSize = 12.sp)
        }
    }
}

@Composable
fun BattleResultScreen(winner: String, myUsername: String, onBackToLobby: () -> Unit) {
    val isWin = winner.equals(myUsername, ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isWin) Color(0xFF0A2E0A) else Color(0xFF2E0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(if (isWin) "🏆" else "💀", fontSize = 80.sp)
            Text(
                if (isWin) "Victoire !" else "Défaite...",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = if (isWin) PokeYellow else PokeRed
            )
            Text(
                if (isWin) "$winner remporte le combat !" else "$winner gagne le combat.",
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onBackToLobby,
                colors = ButtonDefaults.buttonColors(containerColor = if (isWin) HpGreen else PokeRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retour au lobby", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
