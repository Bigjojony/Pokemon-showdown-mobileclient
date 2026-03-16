package com.pokemonshowdown.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokemonshowdown.app.data.model.ConnectionState
import com.pokemonshowdown.app.ui.theme.*

@Composable
fun LoginScreen(
    connectionState: ConnectionState,
    onConnect: () -> Unit,
    onLoginGuest: (String) -> Unit,
    onLoginAccount: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAccountMode by remember { mutableStateOf(false) }
    val isConnecting = connectionState is ConnectionState.Connecting

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PokeGray, PokeSurface, PokeGrayLight)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logo / Titre
            Text(
                text = "⚡",
                fontSize = 72.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Pokémon Showdown",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PokeYellow,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Client Android",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Card de connexion
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PokeGrayLight),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Toggle Guest / Compte
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PokeGray),
                    ) {
                        listOf("Invité" to false, "Mon compte" to true).forEach { (label, mode) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isAccountMode == mode) PokeRed else Color.Transparent)
                                    .clickable { isAccountMode = mode }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontWeight = if (isAccountMode == mode) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Champ pseudo
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Pseudo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PokeYellow,
                            focusedLabelColor = PokeYellow,
                            cursorColor = PokeYellow,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )

                    // Champ mot de passe (si compte)
                    AnimatedVisibility(visible = isAccountMode) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Mot de passe") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PokeYellow,
                                focusedLabelColor = PokeYellow,
                                cursorColor = PokeYellow,
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            )
                        )
                    }

                    // Message d'erreur
                    if (connectionState is ConnectionState.Error) {
                        Text(
                            text = "❌ ${connectionState.message}",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }

                    // Bouton connexion
                    Button(
                        onClick = {
                            if (!isConnecting) {
                                if (connectionState is ConnectionState.Disconnected) {
                                    onConnect()
                                } else {
                                    if (isAccountMode) {
                                        onLoginAccount(username, password)
                                    } else {
                                        onLoginGuest(username.ifBlank { "Guest${(1000..9999).random()}" })
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PokeRed),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isConnecting
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            val label = when (connectionState) {
                                is ConnectionState.Disconnected -> "Se connecter au serveur"
                                is ConnectionState.Connecting -> "Connexion..."
                                is ConnectionState.Connected -> if (isAccountMode) "Connexion compte" else "Jouer en invité"
                                is ConnectionState.Error -> "Réessayer"
                            }
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    // Hint
                    if (connectionState is ConnectionState.Connected) {
                        Text(
                            text = "✅ Serveur connecté ! Entrez votre pseudo pour jouer.",
                            color = HpGreen,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Client non officiel • Données via Pokémon Showdown",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.3f),
                textAlign = TextAlign.Center
            )
        }
    }
}
