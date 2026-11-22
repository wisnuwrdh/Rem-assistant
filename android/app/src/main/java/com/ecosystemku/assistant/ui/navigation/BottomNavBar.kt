package com.ecosystemku.assistant.ui.navigation

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystemku.assistant.ui.theme.AccentBlue
import com.ecosystemku.assistant.ui.theme.BorderSubtle
import com.ecosystemku.assistant.ui.theme.SecondarySurface
import com.ecosystemku.assistant.ui.theme.TextMuted
import com.ecosystemku.assistant.ui.theme.UiSans

enum class AppTab(val label: String, val icon: ImageVector) {
    ASISTEN("Asisten", Icons.Outlined.ChatBubbleOutline),
    APPS("Apps", Icons.Outlined.GridView),
    SETELAN("Setelan", Icons.Outlined.Settings)
}

@Composable
fun EcosystemBottomNavBar(
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    onAsistenDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var lastAsistenTapMs by remember { mutableLongStateOf(0L) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SecondarySurface.copy(alpha = 0.94f))
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val tint = if (isSelected) AccentBlue else TextMuted
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            if (tab == AppTab.ASISTEN && isSelected) {
                                val now = SystemClock.uptimeMillis()
                                if (now - lastAsistenTapMs <= 350L) {
                                    lastAsistenTapMs = 0L
                                    onAsistenDoubleTap()
                                } else {
                                    lastAsistenTapMs = now
                                }
                            } else {
                                lastAsistenTapMs = 0L
                                onSelectTab(tab)
                            }
                        }
                        .padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Garis indikator atas menyala (active tab)
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(3.dp)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .shadow(8.dp, RoundedCornerShape(50), ambientColor = AccentBlue, spotColor = AccentBlue)
                                        .clip(RoundedCornerShape(50))
                                        .background(AccentBlue)
                                } else {
                                    Modifier
                                }
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = tab.label,
                        fontFamily = UiSans,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = tint
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Titik indikator bawah menyala
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .clip(CircleShape)
                                        .background(AccentBlue)
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }
    }
}

