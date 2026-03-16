package com.pokemonshowdown.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import com.pokemonshowdown.app.data.model.ConnectionState
import com.pokemonshowdown.app.ui.screens.*
import com.pokemonshowdown.app.ui.theme.PokemonShowdownTheme
import com.pokemonshowdown.app.viewmodel.ShowdownViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ShowdownViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PokemonShowdownTheme {
                PokemonShowdownApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PokemonShowdownApp(viewModel: ShowdownViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()
    val isInBattle by viewModel.isInBattle.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val battleState by viewModel.battleState.collectAsState()
    val currentMoves by viewModel.currentMoves.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()

    when {
        // En combat
        isInBattle || battleState.isOver -> {
            BattleScreen(
                battleState = battleState,
                currentMoves = currentMoves,
                onUseMove = viewModel::useMove,
                onSwitch = viewModel::switchPokemon,
                onForfeit = viewModel::forfeit,
                onSendChat = viewModel::sendChat,
                onBackToLobby = {
                    viewModel.disconnect()
                    viewModel.connect()
                }
            )
        }

        // Connecté → Lobby
        connectionState is ConnectionState.Connected -> {
            HomeScreen(
                username = (connectionState as ConnectionState.Connected).username,
                isSearching = isSearching,
                selectedFormat = selectedFormat,
                onSearchBattle = viewModel::searchBattle,
                onCancelSearch = viewModel::cancelSearch,
                onFormatChange = viewModel::setFormat,
                onLogout = viewModel::disconnect
            )
        }

        // Non connecté → Login
        else -> {
            LoginScreen(
                connectionState = connectionState,
                onConnect = viewModel::connect,
                onLoginGuest = viewModel::loginAsGuest,
                onLoginAccount = viewModel::loginWithAccount
            )
        }
    }
}
