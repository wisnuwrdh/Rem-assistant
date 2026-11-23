package com.ecosystemku.assistant.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystemku.assistant.data.local.KronikEntryEntity
import com.ecosystemku.assistant.data.local.LinkkuLinkEntity
import com.ecosystemku.assistant.ui.apps.cucimoney.CuciMoneyHelpers
import com.ecosystemku.assistant.ui.apps.cucimoney.CuciMoneyUiState
import com.ecosystemku.assistant.ui.apps.dailyos.DailyOsHelpers
import com.ecosystemku.assistant.ui.apps.dailyos.DailyOsUiState
import com.ecosystemku.assistant.ui.apps.ideku.IdekuUiState
import com.ecosystemku.assistant.ui.apps.kronik.KronikHelpers
import com.ecosystemku.assistant.ui.apps.linkku.LinkkuHelpers
import com.ecosystemku.assistant.ui.theme.AppCuciMoney
import com.ecosystemku.assistant.ui.theme.AppCuciMoneyLight
import com.ecosystemku.assistant.ui.theme.AppDailyOs
import com.ecosystemku.assistant.ui.theme.AppIdeKu
import com.ecosystemku.assistant.ui.theme.AppKronik
import com.ecosystemku.assistant.ui.theme.AppLinkKu
import com.ecosystemku.assistant.ui.theme.BorderLight
import com.ecosystemku.assistant.ui.theme.BorderSubtle
import com.ecosystemku.assistant.ui.theme.DominantBg
import com.ecosystemku.assistant.ui.theme.EditorialSerif
import com.ecosystemku.assistant.ui.theme.ElevatedSurface
import com.ecosystemku.assistant.ui.theme.JournalQuoteStyle
import com.ecosystemku.assistant.ui.theme.SecondarySurface
import com.ecosystemku.assistant.ui.theme.TextMuted
import com.ecosystemku.assistant.ui.theme.TextPrimary
import com.ecosystemku.assistant.ui.theme.TextSubtle
import com.ecosystemku.assistant.ui.theme.UiSans
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tab 2 — Apps Hub.
 * Semua sub-app terhubung ke SQLite Room dan sinkron dengan Asisten Rem.
 */
@Composable
fun AppsHubScreen(
    profileName: String = "Mahfud",
    kronikEntries: List<KronikEntryEntity> = emptyList(),
    linkkuLinks: List<LinkkuLinkEntity> = emptyList(),
    cuciMoneyState: CuciMoneyUiState = CuciMoneyUiState(),
    idekuState: IdekuUiState = IdekuUiState(),
    dailyOsState: DailyOsUiState = DailyOsUiState(),
    onOpenKronik: () -> Unit = {},
    onOpenLinkku: () -> Unit = {},
    onOpenCuciMoney: () -> Unit = {},
    onOpenIdeku: () -> Unit = {},
    onOpenDailyOs: () -> Unit = {},
    onToggleCuciVisibility: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val todayLabel = remember {
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale("in", "ID"))
            .format(Date())
            .replaceFirstChar { it.uppercase() }
    }
    val timeGreeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..10 -> "Selamat Pagi"
            in 11..14 -> "Selamat Siang"
            in 15..17 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DominantBg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Editorial Greeting Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = todayLabel,
                        fontFamily = UiSans,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$timeGreeting, ${profileName.ifBlank { "Tuan" }}",
                        fontFamily = EditorialSerif,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        color = TextPrimary
                    )
                }

                Box(modifier = Modifier.size(42.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(ElevatedSurface)
                            .border(1.dp, BorderLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Profil",
                            tint = TextPrimary.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(AppDailyOs)
                            .border(2.dp, DominantBg, CircleShape)
                    )
                }
            }
        }

        // Section 1: Snapshot Hari Ini
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeaderRow(title = "SNAPSHOT HARI INI", subtitle = "Ringkasan Lintas App")

                // Card A: CuciMoney+ Hero Finance (Room-backed)
                val cuciBook = remember(cuciMoneyState) { CuciMoneyHelpers.activeBook(cuciMoneyState) }
                val cuciBalance = remember(cuciMoneyState) { CuciMoneyHelpers.activeBookBalance(cuciMoneyState) }
                val cuciTodayExp = remember(cuciMoneyState) { CuciMoneyHelpers.activeBookTodayExpense(cuciMoneyState) }

                BentoCard(modifier = Modifier.clickable { onOpenCuciMoney() }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(AppCuciMoney.copy(alpha = 0.2f))
                                    .border(1.dp, AppCuciMoney.copy(alpha = 0.35f), RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = AppCuciMoneyLight,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Total Saldo · ${cuciBook?.name ?: "Personal"}",
                                fontFamily = UiSans,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ElevatedSurface)
                                .border(1.dp, BorderSubtle, CircleShape)
                                .clickable { onToggleCuciVisibility() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (cuciMoneyState.visibility) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = "Sembunyikan Saldo",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = CuciMoneyHelpers.formatRp(cuciBalance, cuciMoneyState.visibility),
                        fontFamily = EditorialSerif,
                        fontSize = 28.sp,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = TextMuted)) { append("Pengeluaran Hari Ini: ") }
                                withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.SemiBold)) {
                                    append(CuciMoneyHelpers.formatRp(cuciTodayExp, cuciMoneyState.visibility))
                                }
                            },
                            fontFamily = UiSans,
                            fontSize = 12.sp,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AppCuciMoney.copy(alpha = 0.2f))
                                .border(1.dp, AppCuciMoney.copy(alpha = 0.35f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "CuciMoney+",
                                fontFamily = UiSans,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF7DB2E8)
                            )
                        }
                    }
                }

                // Card B: Daily OS Habit & To-Do
                val today = DailyOsHelpers.todayStr()
                val dailyStreak = remember(dailyOsState.habits, today) {
                    DailyOsHelpers.calculateOverallStreak(dailyOsState.habits, today)
                }
                val dailyProg = remember(dailyOsState.habits, today) {
                    DailyOsHelpers.calculateTodayProgress(dailyOsState.habits, today)
                }
                val pendingTodoCount = remember(dailyOsState.todos) {
                    dailyOsState.todos.count { !it.done }
                }
                val dailyPct = dailyProg[2]
                val dailyCompletedHabits = dailyProg[3]
                val dailyScheduledHabits = dailyProg[4]

                BentoCard(modifier = Modifier.clickable { onOpenDailyOs() }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppDailyOs.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = AppDailyOs,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Daily OS",
                                fontFamily = UiSans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AppDailyOs.copy(alpha = 0.16f))
                                .border(1.dp, AppDailyOs.copy(alpha = 0.3f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = AppDailyOs,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$dailyStreak Hari Streak",
                                fontFamily = UiSans,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4CC2B3)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (dailyScheduledHabits == 0) "Belum ada habit hari ini" else "$dailyCompletedHabits/$dailyScheduledHabits Habit Selesai",
                            fontFamily = UiSans,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "$dailyPct%",
                            fontFamily = UiSans,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppDailyOs
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ElevatedSurface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((dailyPct / 100f).coerceIn(0f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(AppDailyOs)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tugas Harian",
                            fontFamily = UiSans,
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AppDailyOs)
                            )
                            Text(
                                text = "$pendingTodoCount To-Do Pending",
                                fontFamily = UiSans,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Split Row: IdeKu & Kronik
                val idekuProses = remember(idekuState.ideas) {
                    idekuState.ideas.filter { it.type == "Idea" && it.status == "Proses" }
                }
                val idekuHeadline = if (idekuState.ideas.isEmpty()) {
                    "0 Ide"
                } else {
                    "${idekuProses.size} Proses"
                }
                val idekuSub = when {
                    idekuProses.isNotEmpty() -> "Fokus: ${idekuProses.first().title}"
                    idekuState.ideas.isNotEmpty() -> "${idekuState.ideas.size} ide & wishlist tersimpan"
                    else -> "Ketuk untuk catat ide"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenIdeku() },
                        padding = 14.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppIdeKu.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lightbulb,
                                    contentDescription = null,
                                    tint = AppIdeKu,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "IdeKu",
                                fontFamily = UiSans,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = idekuHeadline,
                            fontFamily = EditorialSerif,
                            fontSize = 22.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = idekuSub,
                            fontFamily = UiSans,
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val latestKronik = kronikEntries.firstOrNull()
                    val kronikPreviewQuote = when {
                        latestKronik == null -> "“Belum ada catatan jurnal…”"
                        latestKronik.title.isNotBlank() -> "“${latestKronik.title}”"
                        latestKronik.content.isNotBlank() -> "“${latestKronik.content.replace('\n', ' ').trim()}”"
                        else -> "“(tanpa judul)”"
                    }
                    val kronikBadge = latestKronik?.moodStatus ?: "Jurnal"
                    val kronikFooter = if (latestKronik != null) {
                        "${KronikHelpers.formatShortIdDate(latestKronik.date)} · ${kronikEntries.size} lembaran"
                    } else {
                        "Ketuk untuk menulis"
                    }

                    BentoCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenKronik() },
                        padding = 14.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppKronik.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                        contentDescription = null,
                                        tint = AppKronik,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "Kronik",
                                    fontFamily = UiSans,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(AppKronik.copy(alpha = 0.16f))
                                    .border(1.dp, AppKronik.copy(alpha = 0.3f), RoundedCornerShape(50))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = kronikBadge,
                                    fontFamily = UiSans,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE0C16B),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = kronikPreviewQuote,
                            style = JournalQuoteStyle,
                            color = TextPrimary.copy(alpha = 0.95f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = kronikFooter,
                            fontFamily = UiSans,
                            fontSize = 10.5.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Card C: LinkKu Snapshot
                val publicLinks = remember(linkkuLinks) { linkkuLinks.filter { !it.isPrivate } }
                val latestLink = publicLinks.firstOrNull()
                val linkHeadline = latestLink?.let {
                    it.title.ifBlank { LinkkuHelpers.extractDomain(it.url) }
                } ?: "Belum ada link tersimpan"
                val linkSub = if (latestLink != null) {
                    "${LinkkuHelpers.extractDomain(latestLink.url)} · ${LinkkuHelpers.formatRelativeTime(latestLink.ts)}"
                } else {
                    "Ketuk untuk simpan link favoritmu"
                }

                BentoCard(
                    modifier = Modifier.clickable { onOpenLinkku() },
                    padding = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppLinkKu.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = AppLinkKu,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "LinkKu",
                                fontFamily = UiSans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AppLinkKu.copy(alpha = 0.16f))
                                .border(1.dp, AppLinkKu.copy(alpha = 0.32f), RoundedCornerShape(50))
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${publicLinks.size} Link",
                                fontFamily = UiSans,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFB794F6)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = linkHeadline,
                        fontFamily = UiSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = linkSub,
                        fontFamily = UiSans,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Section 2: Aplikasi EcosystemKu (4-column grid, 5 modul)
        item {
            val modules = listOf(
                AppTileData("Daily OS", Icons.Outlined.CheckCircle, AppDailyOs, AppDailyOs.copy(alpha = 0.14f), onClick = onOpenDailyOs),
                AppTileData("IdeKu", Icons.Outlined.Lightbulb, AppIdeKu, AppIdeKu.copy(alpha = 0.14f), onClick = onOpenIdeku),
                AppTileData("Kronik", Icons.AutoMirrored.Outlined.MenuBook, AppKronik, AppKronik.copy(alpha = 0.14f), onClick = onOpenKronik),
                AppTileData("CuciMoney+", Icons.Outlined.AccountBalanceWallet, Color(0xFF4D88C2), AppCuciMoney.copy(alpha = 0.16f), onClick = onOpenCuciMoney),
                AppTileData("LinkKu", Icons.Outlined.Link, AppLinkKu, AppLinkKu.copy(alpha = 0.14f), onClick = onOpenLinkku)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeaderRow(title = "APLIKASI ECOSYSTEMKU", subtitle = "5 Modul")

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    modules.chunked(4).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { tile ->
                                AppLauncherTile(tile = tile, modifier = Modifier.weight(1f))
                            }
                            repeat(4 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class AppTileData(
    val name: String,
    val icon: ImageVector,
    val iconTint: Color,
    val containerBg: Color,
    val isExternal: Boolean = false,
    val onClick: (() -> Unit)? = null
)

@Composable
private fun SectionHeaderRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = UiSans,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.9.sp,
            color = TextMuted
        )
        Text(
            text = subtitle,
            fontFamily = UiSans,
            fontSize = 11.sp,
            color = TextSubtle
        )
    }
}

@Composable
private fun BentoCard(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SecondarySurface.copy(alpha = 0.92f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(padding)
    ) {
        content()
    }
}

@Composable
private fun AppLauncherTile(
    tile: AppTileData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SecondarySurface.copy(alpha = 0.9f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .then(if (tile.onClick != null) Modifier.clickable(onClick = tile.onClick) else Modifier)
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        if (tile.isExternal) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(ElevatedSurface)
                    .border(1.dp, BorderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.NorthEast,
                    contentDescription = "Tautan Eksternal",
                    tint = TextMuted,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tile.containerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = tile.name,
                    tint = tile.iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = tile.name,
                fontFamily = UiSans,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (tile.isExternal) TextMuted else TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
