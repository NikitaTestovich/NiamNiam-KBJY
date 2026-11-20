package com.niamniam.kbjy.profile

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@Entity(tableName = "weight_measurements")
data class WeightMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val kilograms: Double,
)

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_measurements ORDER BY date ASC, id ASC")
    fun observeAll(): Flow<List<WeightMeasurement>>

    @Insert suspend fun insert(measurement: WeightMeasurement)
    @Update suspend fun update(measurement: WeightMeasurement)
    @Query("DELETE FROM weight_measurements WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [WeightMeasurement::class], version = 1, exportSchema = false)
abstract class WeightDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao

    companion object {
        @Volatile private var instance: WeightDatabase? = null
        fun get(context: Context): WeightDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, WeightDatabase::class.java, "weight.db")
                .build().also { instance = it }
        }
    }
}

enum class WeightPeriod { WEEK, MONTH, ALL }

fun weightsInPeriod(measurements: List<WeightMeasurement>, period: WeightPeriod, today: LocalDate): List<WeightMeasurement> {
    val firstDate = when (period) {
        WeightPeriod.WEEK -> today.with(java.time.DayOfWeek.MONDAY)
        WeightPeriod.MONTH -> today.withDayOfMonth(1)
        WeightPeriod.ALL -> LocalDate.MIN
    }
    return measurements.filter {
        val date = LocalDate.parse(it.date)
        !date.isBefore(firstDate) && !date.isAfter(today)
    }.sortedWith(compareBy<WeightMeasurement> { it.date }.thenBy { it.id })
}

@Composable
fun WeightScreen(dao: WeightDao, language: AppLanguage, onBack: () -> Unit) {
    val all by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var value by rememberSaveable { mutableStateOf("") }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var period by rememberSaveable { mutableStateOf(WeightPeriod.MONTH) }
    var error by rememberSaveable { mutableStateOf(false) }
    var storageError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val isRussian = language == AppLanguage.RUSSIAN
    fun label(ru: String, en: String) = if (isRussian) ru else en
    val visible = remember(all, period) { weightsInPeriod(all, period, LocalDate.now()) }
    BackHandler(enabled = busy) {}
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onBack, enabled = !busy) { Text(label("Назад", "Back")) }
        Text(label("Вес", "Weight"), style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WeightPeriod.entries.forEach { option ->
                OutlinedButton(onClick = { period = option }) {
                    Text(when (option) {
                        WeightPeriod.WEEK -> label("Неделя", "Week")
                        WeightPeriod.MONTH -> label("Месяц", "Month")
                        WeightPeriod.ALL -> label("Всё", "All")
                    })
                }
            }
        }
        Text(label("Выбранный период: ", "Selected period: ") + when (period) {
            WeightPeriod.WEEK -> label("неделя", "week")
            WeightPeriod.MONTH -> label("месяц", "month")
            WeightPeriod.ALL -> label("всё время", "all time")
        })
        if (visible.isEmpty()) {
            Text(label("Добавьте замер веса", "Add a weight measurement"))
        } else {
            WeightChart(visible)
            if (visible.size >= 2) {
                val delta = visible.last().kilograms - visible.first().kilograms
                Text(label("Изменение: ", "Change: ") + "%.1f".format(delta) + " kg")
            }
        }
        OutlinedTextField(date, { date = it }, label = { Text(label("Дата ГГГГ-ММ-ДД", "Date YYYY-MM-DD")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value, { value = it }, label = { Text(label("Вес, кг", "Weight, kg")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (error) Text(label("Проверьте дату и вес.", "Check the date and weight."), color = MaterialTheme.colorScheme.error)
        if (storageError) Text(label("Не удалось сохранить изменение. Попробуйте ещё раз.", "Could not save the change. Please try again."), color = MaterialTheme.colorScheme.error)
        Button(enabled = !busy, onClick = {
            val parsedDate = try { LocalDate.parse(date) } catch (_: DateTimeParseException) { null }
            val kilograms = value.replace(',', '.').toDoubleOrNull()
            if (parsedDate == null || parsedDate.isAfter(LocalDate.now()) || kilograms == null || !kilograms.isFinite() || kilograms <= 0) {
                error = true
            } else {
                val item = WeightMeasurement(id = editId ?: 0, date = parsedDate.toString(), kilograms = kilograms)
                busy = true
                storageError = false
                scope.launch {
                    try {
                        withContext(Dispatchers.IO + NonCancellable) {
                            if (item.id == 0L) dao.insert(item) else dao.update(item)
                        }
                        date = LocalDate.now().toString()
                        value = ""
                        editId = null
                        error = false
                    } catch (exception: kotlinx.coroutines.CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        storageError = true
                    } finally {
                        busy = false
                    }
                }
            }
        }) { Text(label("Сохранить", "Save")) }
        visible.asReversed().forEach { measurement ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${measurement.date}  ${measurement.kilograms} kg", modifier = Modifier.weight(1f))
                OutlinedButton(enabled = !busy, onClick = {
                    editId = measurement.id
                    date = measurement.date
                    value = measurement.kilograms.toString()
                }) { Text(label("Изменить", "Edit")) }
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    storageError = false
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO + NonCancellable) { dao.delete(measurement.id) }
                            if (editId == measurement.id) { editId = null; value = "" }
                        } catch (exception: kotlinx.coroutines.CancellationException) {
                            throw exception
                        } catch (_: Exception) {
                            storageError = true
                        } finally {
                            busy = false
                        }
                    }
                }) { Text(label("Удалить", "Delete")) }
            }
        }
    }
}

@Composable
private fun WeightChart(measurements: List<WeightMeasurement>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val first = LocalDate.parse(measurements.first().date)
        val last = LocalDate.parse(measurements.last().date)
        val span = ChronoUnit.DAYS.between(first, last).coerceAtLeast(1)
        val minimum = measurements.minOf { it.kilograms }
        val maximum = measurements.maxOf { it.kilograms }
        val range = (maximum - minimum).coerceAtLeast(1.0)
        val margin = 12.dp.toPx()
        val points = measurements.map {
            val days = ChronoUnit.DAYS.between(first, LocalDate.parse(it.date))
            Offset(
                margin + (size.width - 2 * margin) * days.toFloat() / span.toFloat(),
                margin + (size.height - 2 * margin) * (1f - ((it.kilograms - minimum) / range).toFloat()),
            )
        }
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
        }
        points.forEach { drawCircle(color, radius = 4.dp.toPx(), center = it) }
    }
}
