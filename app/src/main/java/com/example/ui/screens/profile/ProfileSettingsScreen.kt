package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.user.UserProfileManager
import com.example.ui.theme.KamakuraSkyBackground
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassFill
import com.example.ui.theme.PaletteCornflower
import com.example.ui.theme.PaletteIceCyan
import com.example.ui.theme.PaletteMintFrost
import com.example.ui.theme.PaletteSoftSky

@Composable
fun ProfileSettingsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val profileManager = remember { UserProfileManager.getInstance(context) }

    val currentName by profileManager.userName.collectAsStateWithLifecycle()
    val currentLocation by profileManager.locationLabel.collectAsStateWithLifecycle()
    val currentLat by profileManager.latitude.collectAsStateWithLifecycle()
    val currentLon by profileManager.longitude.collectAsStateWithLifecycle()
    val currentBio by profileManager.userBio.collectAsStateWithLifecycle()
    val currentHandle by profileManager.telegramHandle.collectAsStateWithLifecycle()
    val currentDirective by profileManager.customDirective.collectAsStateWithLifecycle()
    val currentAvatar by profileManager.avatarEmoji.collectAsStateWithLifecycle()
    val currentStatus by profileManager.currentStatus.collectAsStateWithLifecycle()
    val currentPersona by profileManager.aiPersonaStyle.collectAsStateWithLifecycle()

    var name by remember(currentName) { mutableStateOf(currentName) }
    var location by remember(currentLocation) { mutableStateOf(currentLocation) }
    var latText by remember(currentLat) { mutableStateOf(currentLat.toString()) }
    var lonText by remember(currentLon) { mutableStateOf(currentLon.toString()) }
    var bio by remember(currentBio) { mutableStateOf(currentBio) }
    var handle by remember(currentHandle) { mutableStateOf(currentHandle) }
    var directive by remember(currentDirective) { mutableStateOf(currentDirective) }
    var avatar by remember(currentAvatar) { mutableStateOf(currentAvatar) }
    var status by remember(currentStatus) { mutableStateOf(currentStatus) }
    var persona by remember(currentPersona) { mutableStateOf(currentPersona) }

    KamakuraSkyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("profile_settings_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── TOP HEADER ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PaletteIceCyan.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PaletteCornflower
                        )
                    }
                    Column {
                        Text(
                            text = "Identity & Location",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = PaletteCornflower
                        )
                        Text(
                            text = "Customize Profile, GPS & AI Directives",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                Button(
                    onClick = {
                        val parsedLat = latText.toDoubleOrNull() ?: currentLat
                        val parsedLon = lonText.toDoubleOrNull() ?: currentLon
                        profileManager.updateProfile(
                            name = name,
                            location = location,
                            lat = parsedLat,
                            lon = parsedLon,
                            bio = bio,
                            handle = handle,
                            directive = directive,
                            avatar = avatar,
                            status = status,
                            personaStyle = persona
                        )
                        Toast.makeText(context, "Profile & Location Saved!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaletteCornflower),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // ── 1. AVATAR SELECTOR ──
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose Avatar Icon", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PaletteCornflower)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(UserProfileManager.AVATAR_OPTIONS) { emoji ->
                            val isSelected = avatar == emoji
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) PaletteCornflower else PaletteIceCyan.copy(alpha = 0.5f))
                                    .clickable { avatar = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }

            // ── 2. NAME & BIO ──
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("User Identity", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PaletteCornflower)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Name / Codename") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = handle,
                        onValueChange = { handle = it },
                        label = { Text("Telegram Handle") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio / Tagline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── 3. LOCATION & GPS PRESETS ──
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Location & Default Coordinates", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PaletteCornflower)
                    
                    Text("Quick Location Presets:", fontSize = 11.sp, color = Color.Gray)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(UserProfileManager.DEFAULT_LOCATION_PRESETS) { preset ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (location == preset.label) PaletteCornflower else PaletteMintFrost.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        location = preset.label
                                        latText = preset.lat.toString()
                                        lonText = preset.lon.toString()
                                    }
                            ) {
                                Text(
                                    text = "${preset.icon} ${preset.name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (location == preset.label) Color.White else PaletteCornflower,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location Name / Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text("Latitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lonText,
                            onValueChange = { lonText = it },
                            label = { Text("Longitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── 4. STATUS & AI PERSONA ──
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Live Status & AI Persona", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PaletteCornflower)
                    
                    Text("Current Status:", fontSize = 11.sp, color = Color.Gray)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(UserProfileManager.STATUS_OPTIONS) { st ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (status == st) PaletteCornflower else PaletteIceCyan.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { status = st }
                            ) {
                                Text(
                                    text = st,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (status == st) Color.White else PaletteCornflower,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Text("AI Personality Style:", fontSize = 11.sp, color = Color.Gray)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        UserProfileManager.PERSONA_STYLES.forEach { (styleName, desc) ->
                            val isSelected = persona == styleName
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PaletteCornflower.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, if (isSelected) PaletteCornflower else Color(0xFFE5E7EB)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { persona = styleName }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(styleName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PaletteCornflower)
                                        Text(desc, fontSize = 10.sp, color = Color(0xFF6B7280))
                                    }
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PaletteCornflower, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = directive,
                        onValueChange = { directive = it },
                        label = { Text("Custom AI Directives") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
