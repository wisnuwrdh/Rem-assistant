package com.ecosystemku.assistant.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ecosystemku.assistant.MainActivity
import com.ecosystemku.assistant.R
import com.ecosystemku.assistant.data.local.AssistantDatabase
import com.ecosystemku.assistant.data.local.DailyOsHabitEntity
import com.ecosystemku.assistant.data.local.DailyOsTodoEntity
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DailyOsNotificationHelper {
    const val CHANNEL_ID = "dailyos_reminders"
    const val CHANNEL_PROACTIVE_FINANCE = "proactive_finance"
    const val CHANNEL_PROACTIVE_HABITS = "proactive_habits"
    const val CHANNEL_PROACTIVE_REFLECTION = "proactive_reflection"
    const val CHANNEL_PROACTIVE_IDEAS = "proactive_ideas"

    const val EXTRA_LAUNCH_ACTION = "launch_action"
    const val ACTION_KENALI_AKU = "kenali_aku"
    const val ACTION_OPEN_DAILYOS = "open_dailyos"
    const val ACTION_OPEN_CUCIMONEY = "open_cucimoney"
    const val ACTION_OPEN_IDEKU = "open_ideku"
    const val ACTION_OPEN_KRONIK = "open_kronik"
    const val ACTION_OPEN_CHECKIN = "open_checkin"

    internal const val ACTION_SNOOZE_15M = "com.ecosystemku.assistant.ACTION_NOTIF_SNOOZE_15M"
    internal const val ACTION_MARK_DONE = "com.ecosystemku.assistant.ACTION_NOTIF_MARK_DONE"

    const val REQ_HABIT = 1001
    const val REQ_TODO = 1002
    const val REQ_KG_WEEKLY = 1003

    @Volatile
    private var cachedAvatarBitmap: Bitmap? = null

    fun getProfileName(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences("asisten_prefs", Context.MODE_PRIVATE)
        return prefs.getString("asisten_profile_name", "Tuan")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "Tuan"
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID,
                    "Pengingat Daily OS",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Jadwal pengingat Habit & To-Do harian dari Rem"
                },
                NotificationChannel(
                    CHANNEL_PROACTIVE_FINANCE,
                    "Proaktif · Keuangan (CuciMoney+)",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Peringatan budget dan progres tabungan wishlist dari Rem"
                },
                NotificationChannel(
                    CHANNEL_PROACTIVE_HABITS,
                    "Proaktif · Habit & To-Do",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Penyelamatan streak habit dan pengingat tugas mendesak"
                },
                NotificationChannel(
                    CHANNEL_PROACTIVE_REFLECTION,
                    "Proaktif · Refleksi & Kronik",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Sapaan refleksi saat jurnal vakum atau mood sedang berat"
                },
                NotificationChannel(
                    CHANNEL_PROACTIVE_IDEAS,
                    "Proaktif · IdeKu",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Pengingat ringan untuk ide yang lama tertahan di tahap Proses"
                }
            )
            nm.createNotificationChannels(channels)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun postNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        launchAction: String? = null,
        reminderId: Long = -1L,
        channelId: String = CHANNEL_ID,
        showActionButtons: Boolean = true
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannel(context)

        val formattedTitle = title.replace("•", "·")
        val timeLabel = SimpleDateFormat("HH.mm", Locale.getDefault()).format(Date())

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!launchAction.isNullOrBlank()) {
                putExtra(EXTRA_LAUNCH_ACTION, launchAction)
            }
        }
        val contentPending = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, DailyOsAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_15M
            putExtra("notif_id", notificationId)
            putExtra("reminder_id", reminderId)
            putExtra("title", formattedTitle)
            putExtra("body", body)
            if (!launchAction.isNullOrBlank()) {
                putExtra(EXTRA_LAUNCH_ACTION, launchAction)
            }
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, DailyOsAlarmReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra("notif_id", notificationId)
            putExtra("reminder_id", reminderId)
        }
        val donePending = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val avatarBmp = getOrBuildRemAvatarBitmap(context)

        val compactViews = RemoteViews(context.packageName, R.layout.notif_rem_compact).apply {
            setTextViewText(R.id.notif_title, formattedTitle)
            setTextViewText(R.id.notif_time, timeLabel)
            setTextViewText(R.id.notif_body, body)
            if (avatarBmp != null) {
                setImageViewBitmap(R.id.notif_avatar, avatarBmp)
            }
        }

        val expandedViews = RemoteViews(context.packageName, R.layout.notif_rem_expanded).apply {
            setTextViewText(R.id.notif_title, formattedTitle)
            setTextViewText(R.id.notif_time, timeLabel)
            setTextViewText(R.id.notif_body, body)
            if (avatarBmp != null) {
                setImageViewBitmap(R.id.notif_avatar, avatarBmp)
            }
            if (showActionButtons) {
                setViewVisibility(R.id.notif_actions_row, View.VISIBLE)
                setOnClickPendingIntent(R.id.btn_snooze, snoozePending)
                setOnClickPendingIntent(R.id.btn_done, donePending)
            } else {
                setViewVisibility(R.id.notif_actions_row, View.GONE)
            }
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(formattedTitle)
            .setContentText(body)
            .setCustomContentView(compactViews)
            .setCustomBigContentView(expandedViews)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        }.getOrDefault(false)
    }

    fun postTestNotification(context: Context): Boolean {
        val name = getProfileName(context)
        return postNotification(
            context = context,
            notificationId = 9001,
            title = "Rem · Daily OS",
            body = "$name, notifikasi pengingat Habit & To-Do sudah aktif dan siap mengingatkan $name setiap hari.",
            launchAction = ACTION_OPEN_DAILYOS
        )
    }

    fun syncSchedules(
        context: Context,
        habitEnabled: Boolean,
        habitTime: String,
        todoEnabled: Boolean,
        todoTime: String
    ) {
        ensureChannel(context)
        val name = getProfileName(context)
        scheduleSingle(
            context = context,
            requestCode = REQ_HABIT,
            enabled = habitEnabled,
            timeStr = habitTime,
            title = "Rem · Pengingat Habit",
            body = "$name, waktunya cek dan selesaikan habit Daily OS hari ini ya.",
            launchAction = ACTION_OPEN_DAILYOS
        )
        scheduleSingle(
            context = context,
            requestCode = REQ_TODO,
            enabled = todoEnabled,
            timeStr = todoTime,
            title = "Rem · Pengingat To-Do",
            body = "$name, masih ada daftar tugas To-Do yang menunggu diselesaikan.",
            launchAction = ACTION_OPEN_DAILYOS
        )
    }

    private fun scheduleSingle(
        context: Context,
        requestCode: Int,
        enabled: Boolean,
        timeStr: String,
        title: String,
        body: String,
        launchAction: String? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyOsAlarmReceiver::class.java).apply {
            putExtra("notif_id", requestCode)
            putExtra("title", title)
            putExtra("body", body)
            if (!launchAction.isNullOrBlank()) {
                putExtra(EXTRA_LAUNCH_ACTION, launchAction)
            }
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
        if (!enabled) return

        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 7
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        runCatching {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pending
            )
        }
    }

    fun scheduleOneTimeReminder(
        context: Context,
        reminderId: Long,
        triggerAtMs: Long,
        title: String,
        body: String,
        launchAction: String? = null
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        ensureChannel(context)
        val reqCode = 20_000 + ((reminderId.coerceAtLeast(1L) % 100_000L).toInt())
        val intent = Intent(context, DailyOsAlarmReceiver::class.java).apply {
            putExtra("notif_id", reqCode)
            putExtra("reminder_id", reminderId)
            putExtra("title", title)
            putExtra("body", body)
            if (!launchAction.isNullOrBlank()) {
                putExtra(EXTRA_LAUNCH_ACTION, launchAction)
            }
        }
        val pending = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return runCatching {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
            }
            true
        }.getOrDefault(false)
    }

    fun scheduleSnooze15Minutes(
        context: Context,
        notificationId: Int,
        reminderId: Long,
        title: String,
        body: String,
        launchAction: String?
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        ensureChannel(context)
        val snoozeReqCode = 50_000 + (notificationId.coerceAtLeast(1) % 40_000)
        val triggerAtMs = System.currentTimeMillis() + 15L * 60_000L
        val intent = Intent(context, DailyOsAlarmReceiver::class.java).apply {
            putExtra("notif_id", notificationId)
            putExtra("reminder_id", reminderId)
            putExtra("title", title)
            putExtra("body", body)
            putExtra("is_snoozed", true)
            if (!launchAction.isNullOrBlank()) {
                putExtra(EXTRA_LAUNCH_ACTION, launchAction)
            }
        }
        val pending = PendingIntent.getBroadcast(
            context,
            snoozeReqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
            }
        }
    }

    fun syncWeeklyReflectionSchedule(
        context: Context,
        enabled: Boolean,
        dayOfWeek: Int = Calendar.SUNDAY,
        timeStr: String = "20:00"
    ) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val name = getProfileName(context)
        val intent = Intent(context, DailyOsAlarmReceiver::class.java).apply {
            putExtra("notif_id", REQ_KG_WEEKLY)
            putExtra("title", "Rem · Refleksi Mingguan")
            putExtra("body", "$name, yuk luangkan 2 menit untuk Sesi Kenali Aku mingguan agar peta pola pikirmu makin akurat.")
            putExtra(EXTRA_LAUNCH_ACTION, ACTION_KENALI_AKU)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQ_KG_WEEKLY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
        if (!enabled) return

        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val targetDay = dayOfWeek.coerceIn(Calendar.SUNDAY, Calendar.SATURDAY)

        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, targetDay)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        runCatching {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7L,
                pending
            )
        }
    }

    fun cancelOneTimeReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reqCode = 20_000 + ((reminderId.coerceAtLeast(1L) % 100_000L).toInt())
        val intent = Intent(context, DailyOsAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }

    private fun getOrBuildRemAvatarBitmap(context: Context): Bitmap? {
        cachedAvatarBitmap?.let { if (!it.isRecycled) return it }
        return runCatching {
            val sizePx = 168
            val ringStroke = 7f
            val innerGap = 4f
            val source = BitmapFactory.decodeResource(context.resources, R.drawable.rem_chat_avatar)
                ?: return@runCatching null

            val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val center = sizePx / 2f

            // Outer #5B9CF6 gradient ring
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                    intArrayOf(0xFF80B6FF.toInt(), 0xFF5B9CF6.toInt(), 0xFF4079CE.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(center, center, center - 2f, ringPaint)

            // Cutout inner dark border matching #1A1917 card
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1A1917.toInt()
            }
            canvas.drawCircle(center, center, center - 2f - ringStroke, bgPaint)

            // Cropped circular avatar
            val avatarRadius = center - 2f - ringStroke - innerGap
            val avatarSize = (avatarRadius * 2f).toInt().coerceAtLeast(1)
            val cropped = Bitmap.createBitmap(avatarSize, avatarSize, Bitmap.Config.ARGB_8888)
            val cropCanvas = Canvas(cropped)
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            cropCanvas.drawCircle(avatarRadius, avatarRadius, avatarRadius, maskPaint)
            maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

            val srcMin = minOf(source.width, source.height)
            val srcLeft = (source.width - srcMin) / 2
            val srcTop = (source.height - srcMin) / 2
            val srcRect = Rect(srcLeft, srcTop, srcLeft + srcMin, srcTop + srcMin)
            val dstRect = RectF(0f, 0f, avatarSize.toFloat(), avatarSize.toFloat())
            cropCanvas.drawBitmap(source, srcRect, dstRect, maskPaint)

            canvas.drawBitmap(cropped, center - avatarRadius, center - avatarRadius, null)
            cachedAvatarBitmap = out
            out
        }.getOrNull()
    }

    /**
     * Resolves the notification body for scheduled Daily OS reminders, or returns null
     * when the notification should be suppressed (no pending todos / no incomplete scheduled habits).
     */
    internal fun resolveScheduledReminderBody(
        requestCode: Int,
        profileName: String,
        habits: List<DailyOsHabitEntity>,
        todos: List<DailyOsTodoEntity>,
        today: LocalDate = LocalDate.now()
    ): String? {
        val name = profileName.trim().ifEmpty { "Tuan" }
        return when (requestCode) {
            REQ_TODO -> {
                val pendingTodos = todos.count { !it.done }
                if (pendingTodos > 0) {
                    "$name, masih ada $pendingTodos tugas To-Do yang belum selesai. Mau Rem bantu atur sekarang?"
                } else {
                    null
                }
            }

            REQ_HABIT -> {
                val scheduledToday = habits.filter {
                    !it.archived && ProactiveEvaluator.isHabitScheduled(it, today)
                }
                if (scheduledToday.isEmpty()) return null
                val remainingHabits = scheduledToday.count { !ProactiveEvaluator.isHabitCompletedOnDate(it, today) }
                if (remainingHabits <= 0) return null
                val completedHabits = scheduledToday.size - remainingHabits
                if (completedHabits > 0) {
                    "$name, masih ada $remainingHabits dari ${scheduledToday.size} habit hari ini yang belum selesai. Yuk lanjutkan!"
                } else {
                    "$name, ada $remainingHabits habit terjadwal menunggumu hari ini. Yuk cek sebentar!"
                }
            }

            else -> null
        }
    }
}

class DailyOsAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val id = intent.getIntExtra("notif_id", DailyOsNotificationHelper.REQ_HABIT)
        val reminderId = intent.getLongExtra("reminder_id", -1L)

        when (action) {
            DailyOsNotificationHelper.ACTION_SNOOZE_15M -> {
                NotificationManagerCompat.from(context).cancel(id)
                val title = intent.getStringExtra("title") ?: "Rem · Pengingat"
                val body = intent.getStringExtra("body") ?: "Pengingat dari Rem, ${DailyOsNotificationHelper.getProfileName(context)}."
                val launchAction = intent.getStringExtra(DailyOsNotificationHelper.EXTRA_LAUNCH_ACTION)
                DailyOsNotificationHelper.scheduleSnooze15Minutes(
                    context = context,
                    notificationId = id,
                    reminderId = reminderId,
                    title = title,
                    body = body,
                    launchAction = launchAction
                )
                return
            }

            DailyOsNotificationHelper.ACTION_MARK_DONE -> {
                NotificationManagerCompat.from(context).cancel(id)
                if (reminderId > 0L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            AssistantDatabase.getInstance(context.applicationContext)
                                .remReminderDao()
                                .markFired(reminderId)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
                return
            }
        }

        val name = DailyOsNotificationHelper.getProfileName(context)
        val rawTitle = intent.getStringExtra("title") ?: "Rem · Pengingat"
        val rawBody = intent.getStringExtra("body") ?: "$name, jangan lupa cek Daily OS hari ini ya."
        val launchAction = intent.getStringExtra(DailyOsNotificationHelper.EXTRA_LAUNCH_ACTION)
        val isSnoozed = intent.getBooleanExtra("is_snoozed", false)

        if (!isSnoozed && (id == DailyOsNotificationHelper.REQ_TODO || id == DailyOsNotificationHelper.REQ_HABIT)) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = AssistantDatabase.getInstance(context.applicationContext).dailyOsDao()
                    val resolvedBody = DailyOsNotificationHelper.resolveScheduledReminderBody(
                        requestCode = id,
                        profileName = name,
                        habits = dao.getAllHabits(),
                        todos = dao.getAllTodos()
                    ) ?: return@launch
                    DailyOsNotificationHelper.postNotification(
                        context = context,
                        notificationId = id,
                        title = rawTitle,
                        body = resolvedBody,
                        launchAction = launchAction,
                        reminderId = reminderId
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            DailyOsNotificationHelper.postNotification(
                context = context,
                notificationId = id,
                title = rawTitle,
                body = rawBody,
                launchAction = launchAction,
                reminderId = reminderId
            )
        }
    }
}
