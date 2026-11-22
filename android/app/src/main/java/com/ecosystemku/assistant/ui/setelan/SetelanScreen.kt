package com.ecosystemku.assistant.ui.setelan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystemku.assistant.ui.theme.AccentBlue
import com.ecosystemku.assistant.ui.theme.AppCuciMoney
import com.ecosystemku.assistant.ui.theme.AppDailyOs
import com.ecosystemku.assistant.ui.theme.AppIdeKu
import com.ecosystemku.assistant.ui.theme.AppKronik
import com.ecosystemku.assistant.ui.theme.AppLinkKu
import com.ecosystemku.assistant.ui.theme.BorderLight
import com.ecosystemku.assistant.ui.theme.BorderSubtle
import com.ecosystemku.assistant.ui.theme.DominantBg
import com.ecosystemku.assistant.ui.theme.EditorialSerif
import com.ecosystemku.assistant.ui.theme.ElevatedSurface
import com.ecosystemku.assistant.ui.theme.SecondarySurface
import com.ecosystemku.assistant.ui.theme.SemanticDanger
import com.ecosystemku.assistant.ui.theme.SemanticSuccess
import com.ecosystemku.assistant.ui.theme.SemanticWarning
import com.ecosystemku.assistant.ui.theme.TextMuted
import com.ecosystemku.assistant.ui.theme.TextPrimary
import com.ecosystemku.assistant.ui.theme.TextSubtle
import com.ecosystemku.assistant.ui.theme.UiSans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.ecosystemku.assistant.data.local.UserMemoryEntity

/**
 * Tab 3 — Setelan.
 * Fase 1 aktif: Memori AI & Personalisasi (SQLite Room) dan Reset Data Asisten.
 * Bagian lainnya masih bersifat UI-Only Preview untuk fase berikutnya.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetelanScreen(
    memories: List<UserMemoryEntity> = emptyList(),
    autoLearnMemory: Boolean = true,
    isLearningMemory: Boolean = false,
    onToggleAutoLearn: (Boolean) -> Unit = {},
    onAddManualMemory: (category: String, label: String, value: String) -> Unit = { _, _, _ -> },
    onDeleteMemory: (Long) -> Unit = {},
    onClearMemories: () -> Unit = {},
    onResetKronik: () -> Unit = {},
    onResetLinkku: () -> Unit = {},
    onResetCuciMoney: () -> Unit = {},
    onResetIdeku: () -> Unit = {},
    onResetDailyOs: () -> Unit = {},
    onResetAssistantData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DominantBg)
            .statusBarsPadding()
    ) {
        // Sticky Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DominantBg.copy(alpha = 0.94f))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Setelan",
                fontFamily = EditorialSerif,
                fontSize = 28.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Preferensi, AI, & Backup Ekosistem",
                fontFamily = UiSans,
                fontSize = 12.5.sp,
                color = TextMuted
            )
        }
        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Profil
            item {
                SettingsSection(title = "PROFIL") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(ElevatedSurface)
                                    .border(1.dp, BorderLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Mahfud",
                                    fontFamily = UiSans,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Profil Lokal • Tanpa Cloud Sync",
                                    fontFamily = UiSans,
                                    fontSize = 11.5.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        NestedPill {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Ubah", fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        }
                    }
                }
            }

            // 1B. Memori AI & Personalisasi (AKTIF — SQLite Room Fase 1)
            item {
                var selectedCategory by remember { mutableStateOf("preferensi") }
                var labelInput by remember { mutableStateOf("") }
                var valueInput by remember { mutableStateOf("") }
                val categories = listOf("identitas", "preferensi", "kebiasaan", "tujuan", "catatan")

                SettingsSection(
                    title = "MEMORI AI & PERSONALISASI",
                    trailing = if (isLearningMemory) "Menganalisis…" else "${memories.size} fakta • SQLite",
                    badgeText = if (autoLearnMemory) "Aktif" else null
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Toggle Auto-Belajar dari log chat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleAutoLearn(!autoLearnMemory) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Psychology,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Belajar dari Percakapan",
                                        fontFamily = UiSans,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Ekstrak fakta personal ke SQLite agar Rem mengenal Tuan",
                                        fontFamily = UiSans,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (autoLearnMemory) AccentBlue else ElevatedSurface)
                                    .border(1.dp, if (autoLearnMemory) AccentBlue else BorderLight, RoundedCornerShape(50))
                                    .padding(3.dp),
                                contentAlignment = if (autoLearnMemory) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (autoLearnMemory) DominantBg else TextMuted)
                                )
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                        // Form tambah fakta manual
                        Text(
                            text = "Tambah Fakta Manual",
                            fontFamily = UiSans,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                val active = cat == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) AccentBlue.copy(alpha = 0.2f) else ElevatedSurface)
                                        .border(
                                            1.dp,
                                            if (active) AccentBlue.copy(alpha = 0.55f) else BorderSubtle,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = cat.replaceFirstChar { it.uppercase() },
                                        fontFamily = UiSans,
                                        fontSize = 11.sp,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (active) AccentBlue else TextMuted
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.38f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElevatedSurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 9.dp)
                            ) {
                                if (labelInput.isEmpty()) {
                                    Text("Label (mis. kopi)", fontFamily = UiSans, fontSize = 11.5.sp, color = TextSubtle, maxLines = 1)
                                }
                                BasicTextField(
                                    value = labelInput,
                                    onValueChange = { labelInput = it },
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = UiSans, fontSize = 12.sp, color = TextPrimary),
                                    cursorBrush = SolidColor(AccentBlue),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(0.62f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElevatedSurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 9.dp)
                            ) {
                                if (valueInput.isEmpty()) {
                                    Text("Nilai (mis. Americano tanpa gula)", fontFamily = UiSans, fontSize = 11.5.sp, color = TextSubtle, maxLines = 1)
                                }
                                BasicTextField(
                                    value = valueInput,
                                    onValueChange = { valueInput = it },
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = UiSans, fontSize = 12.sp, color = TextPrimary),
                                    cursorBrush = SolidColor(AccentBlue),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (valueInput.isNotBlank()) AccentBlue else ElevatedSurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                    .clickable(enabled = valueInput.isNotBlank()) {
                                        onAddManualMemory(selectedCategory, labelInput, valueInput)
                                        labelInput = ""
                                        valueInput = ""
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "Simpan Fakta",
                                    tint = if (valueInput.isNotBlank()) DominantBg else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                        // Daftar fakta tersimpan
                        if (memories.isEmpty()) {
                            Text(
                                text = "Belum ada fakta personal tersimpan. Ngobrol dengan Rem atau tambahkan manual di atas.",
                                fontFamily = UiSans,
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp,
                                color = TextSubtle
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                memories.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(ElevatedSurface)
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(11.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.category.uppercase(),
                                                    fontFamily = UiSans,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AccentBlue
                                                )
                                                Text(
                                                    text = "• ${item.factKey}",
                                                    fontFamily = UiSans,
                                                    fontSize = 10.5.sp,
                                                    color = TextSubtle,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = item.factValue,
                                                fontFamily = UiSans,
                                                fontSize = 12.5.sp,
                                                color = TextPrimary
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Hapus Memori",
                                            tint = TextMuted,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .size(16.dp)
                                                .clickable { onDeleteMemory(item.id) }
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SemanticDanger.copy(alpha = 0.08f))
                                    .border(1.dp, SemanticDanger.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .clickable { onClearMemories() }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Bersihkan Semua Memori",
                                    fontFamily = UiSans,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SemanticDanger
                                )
                            }
                        }
                    }
                }
            }

            // 2. AI & API Key
            item {
                SettingsSection(title = "AI & API KEY", trailing = "Android Keystore") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Segmented Control
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Server (Default)", fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextMuted)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(AccentBlue)
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Personal Key", fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DominantBg)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ApiKeyRow("Sumopod")
                            ApiKeyRow("Groq")
                            ApiKeyRow("Fish Audio")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NestedPill {
                                Icon(
                                    imageVector = Icons.Outlined.MonitorHeart,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("Test Koneksi", fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SemanticSuccess.copy(alpha = 0.1f))
                                    .border(1.dp, SemanticSuccess.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SemanticSuccess))
                                Text("Terhubung", fontFamily = UiSans, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = SemanticSuccess)
                            }
                        }
                    }
                }
            }

            // 3. Backup & Restore
            item {
                SettingsSection(title = "BACKUP & RESTORE", trailing = "Format ZIP v3") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = TextMuted)) { append("Backup terakhir: ") }
                                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Medium)) { append("17 Nov 2026, 09:30") }
                                },
                                fontFamily = UiSans,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentBlue)
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Download, contentDescription = null, tint = DominantBg, modifier = Modifier.size(16.dp))
                                Text("Export ZIP Sekarang", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DominantBg)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Upload, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                Text("Import File ZIP (Merge / Replace)", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SemanticWarning.copy(alpha = 0.1f))
                                .border(1.dp, SemanticWarning.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = SemanticWarning, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Ekspor & impor tautan privat LinkKu memerlukan PIN 4-digit untuk enkripsi AES-GCM.",
                                fontFamily = UiSans,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = SemanticWarning.copy(alpha = 0.95f)
                            )
                        }

                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(16.dp))
                                Text("Cloud Backup (Google Drive)", fontFamily = UiSans, fontSize = 12.sp, color = TextMuted)
                            }
                            NestedPill {
                                Text("Segera Hadir (v1.1)", fontFamily = UiSans, fontSize = 10.5.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }

            // 4. Notifikasi
            item {
                SettingsSection(title = "NOTIFIKASI", badgeText = "Izin Aktif") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NotificationRow("Pengingat Habit", "Sesuai hari aktif Daily OS", "07:30")
                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                        NotificationRow("Pengingat To-Do", "Hanya jika ada tugas pending", "20:00")
                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Jadwal lokal perangkat", fontFamily = UiSans, fontSize = 11.sp, color = TextSubtle)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                                Text("Test Notifikasi", fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AccentBlue)
                            }
                        }
                    }
                }
            }

            // 5. Tampilan
            item {
                SettingsSection(title = "TAMPILAN") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mode Warna", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Skala teks mengikuti sistem", fontFamily = UiSans, fontSize = 11.sp, color = TextMuted)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                Text("Sistem", fontFamily = UiSans, fontSize = 12.sp, color = TextMuted)
                            }
                            Box(modifier = Modifier.weight(1f).padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                Text("Terang", fontFamily = UiSans, fontSize = 12.sp, color = TextMuted)
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(AccentBlue)
                                    .padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = DominantBg, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Gelap", fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DominantBg)
                            }
                        }
                    }
                }
            }

            // 6. Data per App
            item {
                val appDataRows = listOf(
                    "Daily OS" to AppDailyOs,
                    "IdeKu" to AppIdeKu,
                    "Kronik" to AppKronik,
                    "CuciMoney+" to AppCuciMoney,
                    "LinkKu" to AppLinkKu,
                    "Asisten" to AccentBlue
                )

                SettingsSection(title = "DATA PER APP", trailing = "Database SQLite") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        appDataRows.forEachIndexed { idx, (name, dotColor) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))
                                    Text(name, fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                }
                                val isResetActive = name == "Asisten" || name == "Kronik" || name == "LinkKu" || name == "CuciMoney+" || name == "IdeKu" || name == "Daily OS"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElevatedSurface)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                        .then(
                                            when (name) {
                                                "Asisten" -> Modifier.clickable { onResetAssistantData() }
                                                "Kronik" -> Modifier.clickable { onResetKronik() }
                                                "LinkKu" -> Modifier.clickable { onResetLinkku() }
                                                "CuciMoney+" -> Modifier.clickable { onResetCuciMoney() }
                                                "IdeKu" -> Modifier.clickable { onResetIdeku() }
                                                "Daily OS" -> Modifier.clickable { onResetDailyOs() }
                                                else -> Modifier
                                            }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Reset", fontFamily = UiSans, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (isResetActive) AccentBlue else TextMuted)
                                }
                            }
                            if (idx < appDataRows.lastIndex) {
                                HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SemanticDanger.copy(alpha = 0.08f))
                                .border(1.dp, SemanticDanger.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = SemanticDanger, modifier = Modifier.size(16.dp))
                                Text("Hapus Semua Data", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = SemanticDanger)
                            }
                        }
                    }
                }
            }

            // 7. Tentang
            item {
                SettingsSection(title = "TENTANG") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("EcosystemKu v1.0.0 (Build 1)", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Android Native Super App • Kotlin Compose", fontFamily = UiSans, fontSize = 11.sp, color = TextMuted)
                            }
                            NestedPill {
                                Text("Android v1", fontFamily = UiSans, fontSize = 10.5.sp, color = TextMuted)
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NestedPill { Text("Live2D Cubism", fontFamily = UiSans, fontSize = 10.5.sp, color = TextMuted) }
                                NestedPill { Text("OFL Fonts", fontFamily = UiSans, fontSize = 10.5.sp, color = TextMuted) }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Repositori GitHub", fontFamily = UiSans, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = AccentBlue)
                                Icon(Icons.Outlined.NorthEast, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    trailing: String? = null,
    badgeText: String? = null,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = UiSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.9.sp,
                color = TextMuted
            )
            if (trailing != null) {
                Text(trailing, fontFamily = UiSans, fontSize = 11.sp, color = TextSubtle)
            }
            if (badgeText != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SemanticSuccess.copy(alpha = 0.1f))
                        .border(1.dp, SemanticSuccess.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SemanticSuccess))
                    Text(badgeText, fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = SemanticSuccess)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun NestedPill(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ElevatedSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        content()
    }
}

@Composable
private fun ApiKeyRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ElevatedSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentBlue))
            Text(label, fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("•••• 8f2a", fontFamily = UiSans, fontSize = 12.sp, color = TextMuted)
            Icon(Icons.Outlined.VisibilityOff, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun NotificationRow(title: String, subtitle: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontFamily = UiSans, fontSize = 11.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NestedPill {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                Text(time, fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }
            // Active switch visual
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AccentBlue)
                    .padding(3.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(DominantBg))
            }
        }
    }
}
