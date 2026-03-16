package com.pokemonshowdown.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokemonshowdown.app.ui.theme.*

val FORMATS = listOf(
    "gen9randombattle" to "Gen 9 Random Battle",
    "gen9ou" to "Gen 9 OU",
    "gen9uu" to "Gen 9 UU",
    "gen9monotype" to "Gen 9 Monotype",
    "gen8randombattle" to "Gen 8 Random Battle",
    "gen8ou" to "Gen 8 OU",
    "gen7randombattle" to "Gen 7 Random Battle",
    "gen6randombattle" to "Gen 6 Random Battle",
)

@Composable
fun HomeScreen(
    username: String,
    isSearching: Boolean,
    selectedFormat: String,
    onSearchBattle: () -> Unit,
    onCancelSearch: () -> Unit,
    onFormatChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    var showFormatPicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PokeGray, PokeGrayLight)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Bonjour,", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                    Text(username, color = PokeYellow, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Déconnexion", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Pokéball animée / état recherche
            AnimatedContent(targetState = isSearching) { searching ->
                if (searching) {
                    SearchingCard(onCancelSearch)
                } else {
                    ReadyCard()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sélecteur de format
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PokeGrayLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Format de combat", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PokeGray)
                            .clickable { showFormatPicker = !showFormatPicker }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            FORMATS.find { it.first == selectedFormat }?.second ?: selectedFormat,
                            color = PokeYellow,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            if (showFormatPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = PokeYellow
                        )
                    }

                    AnimatedVisibility(visible = showFormatPicker) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            FORMATS.forEach { (id, name) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (id == selectedFormat) PokeRed.copy(alpha = 0.3f) else Color.Transparent)
                                        .clickable {
                                            onFormatChange(id)
                                            showFormatPicker = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (id == selectedFormat) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = PokeRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(name, color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bouton chercher combat
            if (!isSearching) {
                Button(
                    onClick = onSearchBattle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PokeRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.SportsKabaddi, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chercher un combat !", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats rapides
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("🎮", "Format", FORMATS.find { it.first == selectedFormat }?.second?.take(12) ?: "—", Modifier.weight(1f))
                StatCard("🌐", "Serveur", "En ligne", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SearchingCard(onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PokeSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = PokeYellow, strokeWidth = 3.dp)
            Text("Recherche d'adversaire...", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            TextButton(onClick = onCancel) {
                Text("Annuler", color = PokeRed)
            }
        }
    }
}

@Composable
fun ReadyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PokeSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚡", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Prêt à combattre !",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Text(
                "Choisissez votre format et lancez la recherche",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun StatCard(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PokeGrayLight),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
