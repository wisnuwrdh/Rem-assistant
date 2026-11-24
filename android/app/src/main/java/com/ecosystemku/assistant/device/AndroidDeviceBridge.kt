package com.ecosystemku.assistant.device

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.resume

sealed interface DeviceActionEvent {
    data class OpenUrl(val url: String) : DeviceActionEvent
    data class ShareText(val text: String) : DeviceActionEvent
    data class SetAlarm(val hour: Int, val minute: Int, val label: String) : DeviceActionEvent
    data class SetTimer(val seconds: Int, val label: String) : DeviceActionEvent
    data class AddCalendarEvent(
        val title: String,
        val startMs: Long,
        val endMs: Long,
        val location: String,
        val description: String
    ) : DeviceActionEvent
    data class RequestPermission(val permission: String, val reasonLabel: String) : DeviceActionEvent
}

object AndroidDeviceBridge {

    fun executeAction(context: Context, event: DeviceActionEvent): String? {
        return when (event) {
            is DeviceActionEvent.OpenUrl -> openSafeUrl(context, event.url)
            is DeviceActionEvent.ShareText -> sharePlainText(context, event.text)
            is DeviceActionEvent.SetAlarm -> launchSystemAlarm(context, event.hour, event.minute, event.label)
            is DeviceActionEvent.SetTimer -> launchSystemTimer(context, event.seconds, event.label)
            is DeviceActionEvent.AddCalendarEvent -> launchAddCalendarEvent(
                context = context,
                title = event.title,
                startMs = event.startMs,
                endMs = event.endMs,
                location = event.location,
                description = event.description
            )
            is DeviceActionEvent.RequestPermission -> null
        }
    }

    fun openSafeUrl(context: Context, rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        val uri = runCatching { Uri.parse(trimmed) }.getOrNull()
            ?: return "URL tidak valid"
        val scheme = uri.scheme?.lowercase(Locale.ROOT).orEmpty()
        if (scheme != "http" && scheme != "https") {
            return "Hanya URL http/https yang diizinkan"
        }
        if (uri.host.isNullOrBlank()) {
            return "Host URL tidak valid"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            null
        } catch (_: ActivityNotFoundException) {
            "Tidak ada browser yang dapat membuka URL"
        } catch (e: Exception) {
            e.message ?: "Gagal membuka URL"
        }
    }

    fun sharePlainText(context: Context, text: String): String? {
        val clean = text.trim()
        if (clean.isEmpty()) return "Teks bagikan kosong"
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, clean)
            }
            val chooser = Intent.createChooser(sendIntent, "Bagikan dari Rem").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            null
        } catch (e: Exception) {
            e.message ?: "Gagal membuka menu bagikan"
        }
    }

    fun launchSystemAlarm(context: Context, hour: Int, minute: Int, label: String): String? {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                if (label.isNotBlank()) {
                    putExtra(AlarmClock.EXTRA_MESSAGE, label.take(80))
                }
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            null
        } catch (_: ActivityNotFoundException) {
            "Tidak ada aplikasi Jam yang mendukung set alarm"
        } catch (e: Exception) {
            e.message ?: "Gagal membuka pengaturan alarm"
        }
    }

    fun launchSystemTimer(context: Context, seconds: Int, label: String): String? {
        val safeSec = seconds.coerceIn(1, 86_400)
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, safeSec)
                if (label.isNotBlank()) {
                    putExtra(AlarmClock.EXTRA_MESSAGE, label.take(80))
                }
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            null
        } catch (_: ActivityNotFoundException) {
            "Tidak ada aplikasi Jam yang mendukung set timer"
        } catch (e: Exception) {
            e.message ?: "Gagal membuka pengaturan timer"
        }
    }

    fun launchAddCalendarEvent(
        context: Context,
        title: String,
        startMs: Long,
        endMs: Long,
        location: String,
        description: String
    ): String? {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title.ifBlank { "Jadwal dari Rem" })
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs.coerceAtLeast(startMs + 1_800_000L))
                if (location.isNotBlank()) {
                    putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                }
                if (description.isNotBlank()) {
                    putExtra(CalendarContract.Events.DESCRIPTION, description)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            null
        } catch (_: ActivityNotFoundException) {
            "Tidak ada aplikasi Kalender di perangkat"
        } catch (e: Exception) {
            e.message ?: "Gagal membuka Kalender"
        }
    }

    fun readBatteryStatus(context: Context): String {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val intent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val pct = run {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt()
            else bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 } ?: -1
        }
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1

        val statusStr = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> {
                val src = when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Nirkabel"
                    else -> "Charger"
                }
                "Sedang diisi ($src)"
            }
            BatteryManager.BATTERY_STATUS_FULL -> "Penuh"
            BatteryManager.BATTERY_STATUS_DISCHARGING, BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Tidak sedang diisi"
            else -> "Status tidak diketahui"
        }
        val pctLabel = if (pct >= 0) "$pct%" else "tidak terbaca"
        val tempLabel = if (tempTenths > 0) ", suhu ${String.format(Locale.US, "%.1f", tempTenths / 10f)}°C" else ""
        return "Baterai perangkat: $pctLabel ($statusStr$tempLabel)."
    }

    fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun readCalendarEvents(context: Context, targetDateStr: String? = null): String = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission(context)) {
            return@withContext "Izin membaca Kalender (READ_CALENDAR) belum diberikan. Sampaikan ke Tuan untuk mengizinkan akses Kalender agar Rem bisa mengecek jadwal."
        }
        val zone = ZoneId.systemDefault()
        val parsedDate = targetDateStr?.trim()?.takeIf { Regex("""^\d{4}-\d{2}-\d{2}$""").matches(it) }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val startDate = parsedDate ?: LocalDate.now(zone)
        val endDate = if (parsedDate != null) parsedDate.plusDays(1) else startDate.plusDays(7)
        val startMs = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = endDate.atStartOfDay(zone).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION
        )

        val items = mutableListOf<String>()
        val timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

        runCatching {
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            android.content.ContentUris.appendId(builder, startMs)
            android.content.ContentUris.appendId(builder, endMs)
            context.contentResolver.query(
                builder.build(),
                projection,
                "${CalendarContract.Instances.VISIBLE} = 1",
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val idxTitle = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val idxBegin = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val idxEnd = cursor.getColumnIndex(CalendarContract.Instances.END)
                val idxAllDay = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val idxLoc = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                while (cursor.moveToNext() && items.size < 20) {
                    val title = if (idxTitle >= 0) cursor.getString(idxTitle).orEmpty().ifBlank { "(Tanpa judul)" } else "(Tanpa judul)"
                    val begin = if (idxBegin >= 0) cursor.getLong(idxBegin) else 0L
                    val end = if (idxEnd >= 0) cursor.getLong(idxEnd) else begin
                    val allDay = idxAllDay >= 0 && cursor.getInt(idxAllDay) == 1
                    val loc = if (idxLoc >= 0) cursor.getString(idxLoc).orEmpty().trim() else ""
                    val whenLabel = if (allDay) {
                        "${Instant.ofEpochMilli(begin).atZone(zone).toLocalDate().format(dateFmt)} (seharian)"
                    } else {
                        val bZdt = Instant.ofEpochMilli(begin).atZone(zone)
                        val eZdt = Instant.ofEpochMilli(end).atZone(zone)
                        "${bZdt.format(timeFmt)}-${String.format(Locale.US, "%02d:%02d", eZdt.hour, eZdt.minute)}"
                    }
                    val locPart = if (loc.isNotBlank()) " @ $loc" else ""
                    items.add("- $whenLabel: $title$locPart")
                }
            }
        }.onFailure { err ->
            return@withContext "Gagal membaca kalender perangkat: ${err.message ?: "error"}"
        }

        if (items.isEmpty()) {
            val rangeDesc = if (parsedDate != null) "tanggal $startDate" else "7 hari ke depan ($startDate s.d. ${endDate.minusDays(1)})"
            "Kalender perangkat: tidak ada jadwal pada $rangeDesc."
        } else {
            val rangeDesc = if (parsedDate != null) "tanggal $startDate" else "7 hari ke depan"
            "Jadwal Kalender Perangkat ($rangeDesc, ${items.size} acara):\n${items.joinToString("\n")}"
        }
    }

    suspend fun readCoarseLocation(context: Context, optInEnabled: Boolean): String = withContext(Dispatchers.IO) {
        if (!optInEnabled) {
            return@withContext "Akses lokasi belum diaktifkan. Tuan dapat mengaktifkan 'Izinkan Rem Baca Lokasi' di tab Setelan terlebih dahulu."
        }
        if (!hasLocationPermission(context)) {
            return@withContext "Izin lokasi (ACCESS_COARSE_LOCATION) belum diberikan di perangkat. Sampaikan ke Tuan untuk mengizinkan akses lokasi."
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext "Layanan lokasi perangkat tidak tersedia."

        val providers = buildList {
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) add(LocationManager.NETWORK_PROVIDER)
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }

        val bestLast: Location? = providers.mapNotNull { prov ->
            runCatching {
                @Suppress("MissingPermission")
                lm.getLastKnownLocation(prov)
            }.getOrNull()
        }.maxByOrNull { it.time }

        if (bestLast == null) {
            return@withContext "Lokasi terakhir perangkat belum tersedia (pastikan GPS/Lokasi perangkat aktif)."
        }

        val lat = bestLast.latitude
        val lon = bestLast.longitude
        val placeLabel = resolveGeocoderLabel(context, lat, lon)
        val coordStr = String.format(Locale.US, "%.4f, %.4f", lat, lon)
        if (placeLabel.isNotBlank()) {
            "Perkiraan lokasi perangkat saat ini: $placeLabel (koordinat sekitar $coordStr)."
        } else {
            "Perkiraan lokasi perangkat saat ini: koordinat sekitar $coordStr."
        }
    }

    private suspend fun resolveGeocoderLabel(context: Context, lat: Double, lon: Double): String {
        if (!Geocoder.isPresent()) return ""
        val geocoder = Geocoder(context, Locale("id", "ID"))
        return withTimeoutOrNull(2_500L) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { list ->
                            val addr = list.firstOrNull()
                            val label = listOfNotNull(
                                addr?.subLocality?.takeIf { it.isNotBlank() },
                                addr?.locality?.takeIf { it.isNotBlank() },
                                addr?.subAdminArea?.takeIf { it.isNotBlank() },
                                addr?.adminArea?.takeIf { it.isNotBlank() },
                                addr?.countryName?.takeIf { it.isNotBlank() }
                            ).distinct().joinToString(", ")
                            if (cont.isActive) cont.resume(label)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addr = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                    listOfNotNull(
                        addr?.subLocality?.takeIf { it.isNotBlank() },
                        addr?.locality?.takeIf { it.isNotBlank() },
                        addr?.subAdminArea?.takeIf { it.isNotBlank() },
                        addr?.adminArea?.takeIf { it.isNotBlank() },
                        addr?.countryName?.takeIf { it.isNotBlank() }
                    ).distinct().joinToString(", ")
                }
            }.getOrDefault("")
        }.orEmpty()
    }
}
