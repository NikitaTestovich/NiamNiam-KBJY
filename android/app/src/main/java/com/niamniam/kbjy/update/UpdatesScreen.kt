package com.niamniam.kbjy.update

import com.niamniam.kbjy.BackButton

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun UpdatesScreen(english: Boolean, onBack: () -> Unit,
    check: (suspend (Long) -> ReleaseCheckResult)? = null,
) {
    val context = LocalContext.current
    val packageInfo = remember { context.packageManager.getPackageInfo(context.packageName, 0) }
    @Suppress("DEPRECATION")
    val currentCode = if (Build.VERSION.SDK_INT >= 28) packageInfo.longVersionCode else packageInfo.versionCode.toLong()
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<ReleaseCheckResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun label(ru: String, en: String) = if (english) en else ru
    fun hasNetwork() = context.getSystemService(ConnectivityManager::class.java)?.activeNetwork != null
    val offlineMessage = label("Подключитесь к интернету и повторите попытку.", "Connect to the internet and try again.")
    BackHandler(enabled = busy) {}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackButton(english, enabled = !busy, onClick = onBack)
        Text(label("Обновления", "Updates"), style = MaterialTheme.typography.headlineMedium)
        Text(label("Установлена версия ", "Installed version ") + packageInfo.versionName)
        Text(label("Проверка и загрузка требуют интернета. Дневник работает без сети.", "Checking and downloading require internet. The diary works offline."))
        Button(enabled = !busy, onClick = {
            scope.launch {
                busy = true
                error = null
                try {
                    result = check?.invoke(currentCode)
                        ?: if (hasNetwork()) GitHubReleaseChecker().check(currentCode) else ReleaseCheckResult.Offline
                }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { result = ReleaseCheckResult.Failed }
                finally { busy = false }
            }
        }) { Text(label("Проверить обновления", "Check for updates")) }
        if (busy) Text(label("Подождите…", "Please wait…"))
        when (val state = result) {
            ReleaseCheckResult.NoRelease -> Text(label("Опубликованный релиз пока недоступен.", "A published release is not available yet."))
            ReleaseCheckResult.NoCompatibleUpdate -> Text(label("Подходящих обновлений нет.", "No compatible updates available."))
            ReleaseCheckResult.Offline -> Text(offlineMessage, color = MaterialTheme.colorScheme.error)
            ReleaseCheckResult.Failed -> Text(label("Не удалось связаться с GitHub. Проверьте интернет и повторите попытку.", "Couldn't reach GitHub. Check your connection and try again."), color = MaterialTheme.colorScheme.error)
            is ReleaseCheckResult.Available -> {
                Text(label("Доступна версия ", "Available version ") + state.versionName)
                if (state.releaseNotes.isNotBlank()) Text(state.releaseNotes)
                Button(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            if (!hasNetwork()) {
                                error = offlineMessage
                            } else if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                                error = label("В настройках Android разрешите установку приложений из этого источника. Затем снова нажмите «Скачать и установить».", "In Android settings, allow installs from this source. Then tap Download and install again.")
                                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                            } else context.startActivity(UpdateInstaller(context.applicationContext).prepare(state.asset))
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = if (!hasNetwork()) offlineMessage else label("Не удалось загрузить или проверить APK. Повторите попытку.", "Couldn't download or verify the APK. Try again.") }
                        finally { busy = false }
                    }
                }) { Text(label("Скачать и установить", "Download and install")) }
            }
            null -> Unit
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
