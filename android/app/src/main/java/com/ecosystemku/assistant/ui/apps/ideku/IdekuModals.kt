package com.ecosystemku.assistant.ui.apps.ideku

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ecosystemku.assistant.data.local.CuciWishlistEntity
import com.ecosystemku.assistant.data.local.IdekuIdeaEntity
import com.ecosystemku.assistant.ui.theme.AppIdeKu
import com.ecosystemku.assistant.ui.theme.BorderLight
import com.ecosystemku.assistant.ui.theme.BorderSubtle
import com.ecosystemku.assistant.ui.theme.DominantBg
import com.ecosystemku.assistant.ui.theme.ElevatedSurface
import com.ecosystemku.assistant.ui.theme.SecondarySurface
import com.ecosystemku.assistant.ui.theme.SemanticDanger
import com.ecosystemku.assistant.ui.theme.SemanticSuccess
import com.ecosystemku.assistant.ui.theme.TextMuted
import com.ecosystemku.assistant.ui.theme.TextPrimary
import com.ecosystemku.assistant.ui.theme.TextSubtle
import com.ecosystemku.assistant.ui.theme.UiSans

@Composable
internal fun IdekuTypeSelectorDialog(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text("Pilih Jenis Item", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Apakah ini Idea atau Wishlist?", color = TextMuted, fontSize = 12.5.sp)
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                        .clickable { onSelect("Idea") }
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = AppIdeKu, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Idea", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TextPrimary)
                        .clickable { onSelect("Wishlist") }
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.ShoppingCart, null, tint = DominantBg, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Wishlist", color = DominantBg, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IdekuFormDialog(
    initialType: String,
    editingItem: IdekuIdeaEntity?,
    categories: List<String>,
    existingJalur: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        title: String,
        notes: String,
        category: String,
        priority: String,
        jalur: String,
        price: Long,
        image: String
    ) -> Unit
) {
    val itemType = editingItem?.type ?: initialType
    val isWishlist = itemType == "Wishlist"

    var title by remember(editingItem, itemType) { mutableStateOf(editingItem?.title ?: "") }
    var notes by remember(editingItem, itemType) { mutableStateOf(editingItem?.notes ?: "") }
    var jalur by remember(editingItem, itemType) { mutableStateOf(editingItem?.jalur ?: "") }
    var category by remember(editingItem, categories) {
        mutableStateOf(
            editingItem?.category?.takeIf { it.isNotBlank() }
                ?: if (isWishlist && "Belanja" in categories) "Belanja" else (categories.firstOrNull() ?: "Personal")
        )
    }
    var priority by remember(editingItem) { mutableStateOf(editingItem?.priority ?: "Nanti") }
    var priceDigits by remember(editingItem) {
        mutableStateOf(if ((editingItem?.price ?: 0L) > 0L) editingItem!!.price.toString() else "")
    }
    var selectedImage by remember(editingItem) { mutableStateOf(editingItem?.image?.ifBlank { "bag" } ?: "bag") }
    var iconPickerExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(22.dp))
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            editingItem != null && isWishlist -> "Edit Wishlist"
                            editingItem != null -> "Edit Ide"
                            isWishlist -> "Wishlist Baru"
                            else -> "Idea Baru"
                        },
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ElevatedSurface)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Close, null, tint = TextMuted, modifier = Modifier.size(15.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                IdekuFieldLabel("JUDUL")
                IdekuTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = if (isWishlist) "Nama barang wishlist..." else "Tulis ide kamu..."
                )

                Spacer(modifier = Modifier.height(12.dp))
                IdekuFieldLabel("CATATAN")
                IdekuTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = "Detail, konteks, atau referensi...",
                    singleLine = false,
                    minLines = 3
                )

                if (!isWishlist) {
                    Spacer(modifier = Modifier.height(12.dp))
                    IdekuFieldLabel("JALUR (OPSIONAL)")
                    IdekuTextField(
                        value = jalur,
                        onValueChange = { jalur = it },
                        placeholder = "mis. Ekosistemku, Project X"
                    )
                    if (existingJalur.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            existingJalur.forEach { j ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (jalur == j) AppIdeKu.copy(alpha = 0.22f) else ElevatedSurface)
                                        .border(1.dp, if (jalur == j) AppIdeKu else BorderSubtle, RoundedCornerShape(8.dp))
                                        .clickable { jalur = j }
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(j, color = if (jalur == j) Color.White else TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                IdekuFieldLabel("KATEGORI")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val catChoices = (categories + category).distinct()
                    catChoices.forEach { cat ->
                        val selected = cat == category
                        val dotCol = IdekuHelpers.categoryColor(cat)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) dotCol.copy(alpha = 0.2f) else ElevatedSurface)
                                .border(1.dp, if (selected) dotCol else BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotCol))
                            Text(cat, color = if (selected) TextPrimary else TextMuted, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                IdekuFieldLabel("PRIORITAS")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IdekuHelpers.PRIORITIES.forEach { pri ->
                        val selected = pri == priority
                        val priCol = IdekuHelpers.priorityColor(pri)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) priCol.copy(alpha = 0.16f) else ElevatedSurface)
                                .border(1.dp, if (selected) priCol else BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { priority = pri }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(pri, color = if (selected) priCol else TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (isWishlist) {
                    Spacer(modifier = Modifier.height(12.dp))
                    IdekuFieldLabel("HARGA (RP)")
                    IdekuTextField(
                        value = priceDigits,
                        onValueChange = { priceDigits = it.filter { ch -> ch.isDigit() } },
                        placeholder = "Contoh: 250000",
                        keyboardType = KeyboardType.Number
                    )
                    val parsedPrice = priceDigits.toLongOrNull() ?: 0L
                    if (parsedPrice > 0L) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(IdekuHelpers.formatRp(parsedPrice), color = AppIdeKu, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    IdekuFieldLabel("ICON PRODUK")
                    val curIconDef = IdekuHelpers.WISHLIST_ICONS.firstOrNull { it.key == selectedImage } ?: IdekuHelpers.WISHLIST_ICONS.first()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { iconPickerExpanded = !iconPickerExpanded }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(curIconDef.icon, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                        Text(curIconDef.label, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(if (iconPickerExpanded) "▲" else "▼", color = TextMuted, fontSize = 11.sp)
                    }
                    if (iconPickerExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IdekuHelpers.WISHLIST_ICONS.forEach { def ->
                                val active = def.key == selectedImage
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (active) AppIdeKu.copy(alpha = 0.2f) else ElevatedSurface)
                                        .border(1.dp, if (active) AppIdeKu else BorderSubtle, RoundedCornerShape(9.dp))
                                        .clickable {
                                            selectedImage = def.key
                                            iconPickerExpanded = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(def.icon, def.label, tint = if (active) Color.White else TextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Batal", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    val valid = title.trim().isNotEmpty()
                    Box(
                        modifier = Modifier
                            .weight(1.7f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (valid) TextPrimary else ElevatedSurface)
                            .clickable(enabled = valid) {
                                onSave(
                                    itemType,
                                    title.trim(),
                                    notes.trim(),
                                    category,
                                    priority,
                                    jalur.trim(),
                                    priceDigits.toLongOrNull() ?: 0L,
                                    selectedImage
                                )
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (editingItem != null) "Simpan Perubahan" else if (isWishlist) "Simpan Wishlist" else "Simpan Ide",
                            color = if (valid) DominantBg else TextSubtle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun IdekuItemDetailDialog(
    item: IdekuIdeaEntity,
    savingPot: CuciWishlistEntity?,
    existingJalur: List<String>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onUpdatePriority: (String) -> Unit,
    onSaveMeta: (jalur: String, nextStep: String) -> Unit,
    onPerkaya: () -> Unit,
    onBreakdown: () -> Unit
) {
    var jalurInput by remember(item.id, item.jalur) { mutableStateOf(item.jalur) }
    var nextInput by remember(item.id, item.nextStep) { mutableStateOf(item.nextStep) }
    val catColor = IdekuHelpers.categoryColor(item.category)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(catColor))
                            Text(
                                text = buildString {
                                    append(item.category)
                                    if (item.price > 0L) append(" · ${IdekuHelpers.formatRp(item.price)}")
                                },
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElevatedSurface)
                                .clickable(onClick = onEdit),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Edit, "Edit", tint = TextMuted, modifier = Modifier.size(15.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SemanticDanger.copy(alpha = 0.12f))
                                .clickable(onClick = onDelete),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, "Hapus", tint = SemanticDanger, modifier = Modifier.size(15.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElevatedSurface)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Close, "Tutup", tint = TextMuted, modifier = Modifier.size(15.dp))
                        }
                    }
                }

                if (item.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(item.notes, color = TextPrimary.copy(alpha = 0.88f), fontSize = 13.sp, lineHeight = 20.sp)
                }

                if (savingPot != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    IdekuWishlistSavingsBox(price = item.price, saved = savingPot.saved)
                }

                if (item.type == "Idea") {
                    Spacer(modifier = Modifier.height(14.dp))
                    IdekuFieldLabel("JALUR")
                    IdekuTextField(
                        value = jalurInput,
                        onValueChange = { jalurInput = it },
                        placeholder = "Kelompokkan ide dalam satu jalur"
                    )
                    if (existingJalur.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            existingJalur.forEach { j ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(if (jalurInput == j) AppIdeKu.copy(alpha = 0.2f) else ElevatedSurface)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(7.dp))
                                        .clickable { jalurInput = j }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(j, color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    IdekuFieldLabel("LANGKAH BERIKUTNYA")
                    IdekuTextField(
                        value = nextInput,
                        onValueChange = { nextInput = it },
                        placeholder = "Satu tindakan kecil untuk hari ini"
                    )
                    if (jalurInput.trim() != item.jalur || nextInput.trim() != item.nextStep) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppIdeKu)
                                .clickable { onSaveMeta(jalurInput.trim(), nextInput.trim()) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Simpan Jalur & Langkah", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                IdekuFieldLabel("STATUS")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    IdekuHelpers.STATUSES.forEach { st ->
                        val active = item.status == st
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) TextPrimary else ElevatedSurface)
                                .border(1.dp, if (active) TextPrimary else BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { onUpdateStatus(st) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(st, color = if (active) DominantBg else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                IdekuFieldLabel("PRIORITAS")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    IdekuHelpers.PRIORITIES.forEach { pri ->
                        val active = item.priority == pri
                        val col = IdekuHelpers.priorityColor(pri)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) col.copy(alpha = 0.16f) else ElevatedSurface)
                                .border(1.dp, if (active) col else BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { onUpdatePriority(pri) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(pri, color = if (active) col else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                            .clickable(onClick = onPerkaya)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Perkaya Ide", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                            .clickable(onClick = onBreakdown)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Breakdown", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun IdekuWishlistSavingsBox(price: Long, saved: Long) {
    val full = price > 0L && saved >= price
    val pct = if (price > 0L) (if (full) 100 else ((saved * 100L) / price).toInt().coerceIn(0, 99)) else 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ElevatedSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tabungan di CuciMoney+", color = TextMuted, fontSize = 11.sp)
            Text(
                text = if (price > 0L) "${IdekuHelpers.formatRp(saved)} / ${IdekuHelpers.formatRp(price)} · $pct%" else IdekuHelpers.formatRp(saved),
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (price > 0L) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BorderSubtle)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pct / 100f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (full) Color(0xFFF59E0B) else Color(0xFF10B981))
                )
            }
        }
        if (full) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Target tercapai — cairkan lewat menu Tabungan di CuciMoney+", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
internal fun IdekuAiResultDialog(
    isBreakdown: Boolean,
    ideaTitle: String,
    isLoading: Boolean,
    resultText: String?,
    errorText: String?,
    originalNotes: String,
    onRetry: () -> Unit,
    onSavePerkaya: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(resultText) { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isBreakdown) "Prompt Breakdown" else "Hasil Perkaya Ide", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(ideaTitle, color = TextMuted, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(ElevatedSurface).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Close, null, tint = TextMuted, modifier = Modifier.size(15.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when {
                    isLoading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = AppIdeKu, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                            Text("Rem AI sedang menyusun...", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    errorText != null -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(errorText, color = SemanticDanger, fontSize = 12.5.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElevatedSurface)
                                    .clickable(onClick = onRetry)
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text("Coba Lagi", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    resultText != null -> {
                        Column {
                            Text(resultText, color = TextPrimary.copy(alpha = 0.92f), fontSize = 13.sp, lineHeight = 20.sp)
                            if (!isBreakdown && originalNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ElevatedSurface)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Text("CATATAN SEBELUMNYA", color = TextSubtle, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(originalNotes, color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isBreakdown) "Tutup" else "Buang", color = TextMuted, fontSize = 13.sp)
                }
                val canAct = !isLoading && !resultText.isNullOrBlank()
                if (isBreakdown) {
                    Row(
                        modifier = Modifier
                            .weight(1.5f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (copied) SemanticSuccess else if (canAct) TextPrimary else ElevatedSurface)
                            .clickable(enabled = canAct) {
                                clipboard.setText(AnnotatedString(resultText!!))
                                copied = true
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.ContentCopy, null, tint = DominantBg, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copied) "Tersalin!" else "Salin Prompt", color = DominantBg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (canAct) TextPrimary else ElevatedSurface)
                            .clickable(enabled = canAct) { onSavePerkaya(resultText!!) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Simpan ke Catatan", color = if (canAct) DominantBg else TextSubtle, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun IdekuAiDecisionDialog(
    item: IdekuIdeaEntity,
    decision: IdekuAiDecision,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF7C3AED).copy(alpha = 0.16f))
                        .border(1.dp, Color(0xFF7C3AED).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("AI Recommendation", color = TextMuted, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(decision.color.copy(alpha = 0.12f))
                    .border(1.dp, decision.color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text("KEPUTUSAN", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(decision.label, color = decision.color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(decision.reason, color = TextPrimary.copy(alpha = 0.9f), fontSize = 12.5.sp, lineHeight = 18.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElevatedSurface)
                    .padding(12.dp)
            ) {
                Text("Rekomendasi", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(3.dp))
                Text(decision.recommendation, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(decision.color)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Paham", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun IdekuFocusFullDialog(
    newItem: IdekuIdeaEntity,
    activeFocusList: List<IdekuIdeaEntity>,
    onSwap: (parkId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text("Fokus sudah penuh (${IdekuHelpers.FOCUS_LIMIT}/${IdekuHelpers.FOCUS_LIMIT})", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Untuk mengerjakan “${newItem.title}”, parkir salah satu dulu. Ide yang diparkir aman dan tidak hilang.",
                color = TextMuted,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            activeFocusList.forEach { f ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { onSwap(f.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text("Parkir: ${f.title}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Batal", color = TextMuted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun IdekuManageCategoriesDialog(
    categories: List<String>,
    ideas: List<IdekuIdeaEntity>,
    onAddCategory: (String) -> Boolean,
    onDeleteCategory: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Kelola Kategori", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(ElevatedSurface).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Close, null, tint = TextMuted, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories, key = { it }) { cat ->
                    val count = ideas.count { it.category == cat }
                    val dotCol = IdekuHelpers.categoryColor(cat)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotCol))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cat, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        if (count > 0) {
                            Text("$count ide", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
                        }
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Hapus",
                            tint = if (count == 0) SemanticDanger else TextSubtle.copy(alpha = 0.35f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(enabled = count == 0) { onDeleteCategory(cat) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    IdekuTextField(value = input, onValueChange = { input = it }, placeholder = "Nama kategori baru...")
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (input.isNotBlank()) TextPrimary else ElevatedSurface)
                        .clickable(enabled = input.isNotBlank()) {
                            if (onAddCategory(input)) input = ""
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Text("Tambah", color = if (input.isNotBlank()) DominantBg else TextSubtle, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(TextPrimary)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Selesai", color = DominantBg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun IdekuConfirmDialog(
    title: String,
    subtitle: String,
    confirmLabel: String = "Hapus",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(title, color = TextPrimary, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(subtitle, color = TextMuted, fontSize = 12.5.sp, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Batal", color = TextMuted, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SemanticDanger)
                        .clickable(onClick = onConfirm)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(confirmLabel, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun IdekuFieldLabel(text: String) {
    Text(
        text = text,
        fontFamily = UiSans,
        color = TextMuted,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 5.dp)
    )
}

@Composable
internal fun IdekuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElevatedSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontFamily = UiSans, fontSize = 13.sp, color = TextSubtle)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(fontFamily = UiSans, fontSize = 13.sp, color = TextPrimary),
            cursorBrush = SolidColor(AppIdeKu),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
