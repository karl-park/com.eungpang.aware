package com.eungpang.applocker.presentation.service

import android.app.*
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.eungpang.snstimechecker.R
import com.eungpang.applocker.domain.item.Item
import com.eungpang.applocker.domain.item.Item.Companion.KEY_SERIALIZABLE
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class TimeCheckService : Service() {
    private val _processInfoMap = mutableMapOf<String, Pair<AppInfo, ScheduledFuture<*>>>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val item = intent?.getSerializableExtra(KEY_SERIALIZABLE) as Item? ?: return START_STICKY
        val appInfo = if (_processInfoMap.containsKey(item.packageName)) {
            _processInfoMap[item.packageName]!!.first
        } else {
            AppInfo.from(item, startId)
        }

        when (intent?.action) {
            TimeCheckServiceActions.START_SERVICE -> {
                val notification = TimeCheckerNotification.createNotification(item)
                startForeground(appInfo.notificationId, notification)

                startTimer(appInfo)
            }

            TimeCheckServiceActions.STOP_SERVICE -> {
                stopTimer(appInfo)

                if (_processInfoMap.entries.isEmpty()) {
                    stopForeground(true)
                    stopSelf()
                } else {
                    stopForeground(false)
                }
            }

            TimeCheckServiceActions.PAUSE_NOTI -> {
                pauseTimer(appInfo)
            }

            TimeCheckServiceActions.RESUME_NOTI -> {
                resumeTimer(appInfo)
            }

            TimeCheckServiceActions.STOP_NOTI -> {
                stopTimer(appInfo)

                if (_processInfoMap.entries.isEmpty()) {
                    stopForeground(true)
                    stopSelf()
                } else {
                    stopForeground(false)
                }
            }

            else -> {
                // do nothing
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private val scheduledThreadPoolExecutor = Executors.newScheduledThreadPool(5)
    private fun startTimer(appInfo: AppInfo) {
        val scheduledFuture = scheduledThreadPoolExecutor.scheduleAtFixedRate({
            val process = _processInfoMap[appInfo.name] ?: return@scheduleAtFixedRate

            if (process.first.isPaused) return@scheduleAtFixedRate

            val elapsedTime = if (process.first.lastResumedTime == 0L) {
                System.currentTimeMillis() - process.first.startTime
            } else {
                System.currentTimeMillis() - process.first.lastResumedTime + process.first.lastElapsedTime
            }

            val newAppInfo = process.first.copy(
                    lastResumedTime = System.currentTimeMillis(),
                    lastElapsedTime = elapsedTime
            )

            _processInfoMap[newAppInfo.name] = _processInfoMap[newAppInfo.name]!!.copy(first = newAppInfo)

            TimeCheckerNotification.updateNotification(newAppInfo)
        }, 0, 2, TimeUnit.SECONDS)

        _processInfoMap[appInfo.name] = Pair(appInfo, scheduledFuture)

        Toast.makeText(applicationContext, "${appInfo.name} Start", Toast.LENGTH_SHORT).show()
    }

    private fun pauseTimer(appInfo: AppInfo) {
        val process = _processInfoMap[appInfo.name] ?: return
        val newAppInfo = process.first.copy(
                isPaused = true
        )

        _processInfoMap[newAppInfo.name] = _processInfoMap[newAppInfo.name]!!.copy(first = newAppInfo)
        TimeCheckerNotification.updateNotification(newAppInfo)

        Toast.makeText(applicationContext, "${appInfo.name} Paused", Toast.LENGTH_SHORT).show()
    }

    private fun resumeTimer(appInfo: AppInfo) {
        val process = _processInfoMap[appInfo.name] ?: return
        val newAppInfo = process.first.copy(
                lastResumedTime = System.currentTimeMillis(),
                isPaused = false
        )

        _processInfoMap[newAppInfo.name] = _processInfoMap[newAppInfo.name]!!.copy(first = newAppInfo)

        Toast.makeText(applicationContext, "${appInfo.name} Resumed", Toast.LENGTH_SHORT).show()
    }

    private fun stopTimer(appInfo: AppInfo) {
        val process = _processInfoMap[appInfo.name]
        if (process == null) {
            TimeCheckerNotification.cancelNotification(appInfo.notificationId)
            return
        }

        if (!process.second.isCancelled) {
            process.second.cancel(false)
        }

        _processInfoMap.remove(appInfo.name)
        TimeCheckerNotification.cancelNotification(appInfo.notificationId)

        Toast.makeText(applicationContext, "${appInfo.name} Stop", Toast.LENGTH_SHORT).show()
    }
}

object TimeCheckServiceActions {
    private const val PREFIX = "com.eungpang.snstimechecker.presentation.service.action."
    const val PAUSE_NOTI = PREFIX + "pause"
    const val RESUME_NOTI = PREFIX + "resume"
    const val STOP_NOTI = PREFIX + "stop"

    const val START_SERVICE = PREFIX + "startforeground"
    const val STOP_SERVICE = PREFIX + "stopforeground"
}

object TimeCheckerNotification {
    lateinit var applicationContext: Context

    private const val CHANNEL_ID = "foreground_service_channel"

    private var notificationBuilder: NotificationCompat.Builder? = null

    fun init(app: Application) {
        applicationContext = app.applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                    CHANNEL_ID,
                    "SNS Time Checker Channel",
                    NotificationManager.IMPORTANCE_LOW).apply {
                description = "Time checker channel"
                enableVibration(false)
                enableLights(false)
                setShowBadge(true)
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    fun cancelNotification(notificationId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.cancel(notificationId)
        }
    }

    fun updateNotification(
            appInfo: AppInfo
    ) {
        val elapsedTime = appInfo.lastElapsedTime
        val elapsedTimeString = (elapsedTime / 1_000).toString() + " s"


        if (appInfo.isPaused) {
            // Resume
            val resumeIntent = Intent(applicationContext, TimeCheckService::class.java).apply {
                action = TimeCheckServiceActions.RESUME_NOTI
                putExtra(KEY_SERIALIZABLE, appInfo.item)
            }
            val resumePendingIntent = PendingIntent
                    .getService(applicationContext, appInfo.item.packageName.hashCode() + 1, resumeIntent, FLAG_UPDATE_CURRENT)

            // Stop
            val stopIntent = Intent(applicationContext, TimeCheckService::class.java).apply {
                action = TimeCheckServiceActions.STOP_NOTI
                putExtra(KEY_SERIALIZABLE, appInfo.item)
            }
            val stopPendingIntent = PendingIntent
                    .getService(applicationContext, appInfo.item.packageName.hashCode() + 2, stopIntent, FLAG_UPDATE_CURRENT)

            notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setContentTitle("${appInfo.name} is paused.")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("ElapsedTime: $elapsedTimeString"))
                    .setSmallIcon(R.drawable.ic_noti_hourglass)
                    .setOngoing(true) // true 일경우 알림 리스트에서 클릭하거나 좌우로 드래그해도 사라지지 않음
                    .addAction(NotificationCompat.Action(android.R.drawable.ic_media_play, "Resume", resumePendingIntent))
                    .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Stop", stopPendingIntent))
        } else {
            // Pause
            val pauseNotiIntent = Intent(applicationContext, TimeCheckService::class.java).apply {
                action = TimeCheckServiceActions.PAUSE_NOTI
                putExtra(KEY_SERIALIZABLE, appInfo.item)
            }
            val pauseNotiPendingIntent = PendingIntent
                    .getService(applicationContext, appInfo.item.packageName.hashCode(), pauseNotiIntent, FLAG_UPDATE_CURRENT)

            // Stop
            val stopIntent = Intent(applicationContext, TimeCheckService::class.java).apply {
                action = TimeCheckServiceActions.STOP_NOTI
                putExtra(KEY_SERIALIZABLE, appInfo.item)
            }
            val stopPendingIntent = PendingIntent
                    .getService(applicationContext, appInfo.item.packageName.hashCode() + 2, stopIntent, FLAG_UPDATE_CURRENT)

            notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setContentTitle("${appInfo.name} is launched.")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("ElapsedTime: $elapsedTimeString"))
                    .setSmallIcon(R.drawable.ic_noti_hourglass)
                    .setOngoing(true) // true 일경우 알림 리스트에서 클릭하거나 좌우로 드래그해도 사라지지 않음
                    .addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, "Pause", pauseNotiPendingIntent))
                    .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Stop", stopPendingIntent))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.notify(appInfo.notificationId, notificationBuilder!!.build())
        }
    }

    fun createNotification(
            item: Item
    ): Notification {
        // Pause
        val pauseNotiIntent = Intent(applicationContext, TimeCheckService::class.java).apply {
            action = TimeCheckServiceActions.PAUSE_NOTI
            putExtra(KEY_SERIALIZABLE, item)
        }
        val pauseNotiPendingIntent = PendingIntent
                .getService(applicationContext, item.packageName.hashCode(), pauseNotiIntent, FLAG_UPDATE_CURRENT)

        // Stop
        val stopIntent = Intent(applicationContext, TimeCheckService::class.java).apply {
            action = TimeCheckServiceActions.STOP_NOTI
            putExtra(KEY_SERIALIZABLE, item)
        }
        val stopPendingIntent = PendingIntent
                .getService(applicationContext, item.packageName.hashCode() + 2, stopIntent, FLAG_UPDATE_CURRENT)

        // Notification
        if (notificationBuilder == null) {
            notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentTitle("${item.packageName} is launched.")
                    .setSmallIcon(R.drawable.ic_noti_hourglass)
                    .setOngoing(true) // true 일경우 알림 리스트에서 클릭하거나 좌우로 드래그해도 사라지지 않음
                    .addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, "Pause", pauseNotiPendingIntent))
                    .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Stop", stopPendingIntent))
        }

        return notificationBuilder!!.build()
    }
}

data class AppInfo(
        val name: String,
        val packageName: String,
        val startTime: Long,
        val lastResumedTime: Long,
        val lastElapsedTime: Long,
        val serviceStartId: Int,
        val isPaused: Boolean = false,
        val item: Item
) {
    companion object {
        fun from(item: Item, serviceStartId: Int): AppInfo =
                AppInfo(item.name, item.packageName, System.currentTimeMillis(), 0L, 0L, serviceStartId, false, item)
    }

    val notificationId : Int
        get() = packageName.hashCode()
}

