package com.example.mooddiary

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val moodViewModel: MoodViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var adapter: MoodAdapter
    private lateinit var editTextSearch: EditText
    private lateinit var buttonSearch: ImageButton
    private lateinit var buttonFilter: ImageButton

    private var isAscending = true

    //обновляет статус записи и сохраняет его в файл после завершения операции добавления или редактирования записи
    private val addMoodLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val id = data?.getIntExtra("id", -1) ?: -1
            val isUpdated = data?.getBooleanExtra("isUpdated", false) ?: false
            if (isUpdated && id != -1) {
                val status = "Обновлено: ${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())}"
                adapter.updateStatus(id, status) //вызов метода у адаптера для обновления статуса заметки в списке
                saveStatusToFile(id, status) //вызов сохранения в файл
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        fab = findViewById(R.id.plus)
        editTextSearch = findViewById(R.id.editTextSearch)
        buttonSearch = findViewById(R.id.buttonSearch)
        buttonFilter = findViewById(R.id.buttonFilter)


        adapter = MoodAdapter(listOf(), { mood ->
            moodViewModel.delete(mood) //удаление
        }, { mood ->
            onMoodClick(mood)
        }, this)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // наблюдатель для обновления списка (если произошло изменение)
        moodViewModel.allMoods.observe(this, { moods ->
            moods?.let { adapter.updateMoods(it) }
        })

        //создание заметки
        fab.setOnClickListener {
            val intent = Intent(this, AddMoodActivity::class.java)
            addMoodLauncher.launch(intent)
        }

        buttonSearch.setOnClickListener {
            filterMoods(editTextSearch.text.toString())
        }

        buttonFilter.setOnClickListener {
            toggleFilter()
        }


        //слушатель текста в поиске
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    resetFilter() //если текст пустой
                }
            }
        })

        loadStatusesFromFile()
    }

    private fun onMoodClick(mood: MoodEntry) {
        val intent = Intent(this, AddMoodActivity::class.java)
        intent.putExtra("modeType", "Edit")
        intent.putExtra("date", mood.date)
        intent.putExtra("time", mood.time)
        intent.putExtra("description", mood.description)
        intent.putExtra("mood", mood.mood)
        intent.putExtra("id", mood.id)
        addMoodLauncher.launch(intent)
    }

    //поиск
    private fun filterMoods(query: String) {
        val filteredMoods = moodViewModel.allMoods.value?.filter {
            it.mood.contains(query, ignoreCase = true)
        }
        adapter.updateMoods(filteredMoods ?: listOf())
    }

    //очистка в поиске
    private fun resetFilter() {
        val allMoods = moodViewModel.allMoods.value
        adapter.updateMoods(allMoods ?: listOf())
    }

    //переключатель сортировки
    private fun toggleFilter() {
        val sortedMoods = moodViewModel.allMoods.value?.sortedWith(compareBy(
            { parseDate(it.date) },
            { it.time }
        ))

        if (isAscending) {
            adapter.updateMoods(sortedMoods ?: listOf())
        } else {
            adapter.updateMoods(sortedMoods?.reversed() ?: listOf())
        }
        isAscending = !isAscending
    }

    //строку в дату
    private fun parseDate(dateString: String): Date? {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveStatusToFile(id: Int, status: String) {
        val fileName = "status_$id.txt"
        val fileContents = status
        FileOutputStream(File(filesDir, fileName)).use {
            it.write(fileContents.toByteArray())
        }
    }

    private fun loadStatusesFromFile() {
        val statusMap: MutableMap<Int, String> = mutableMapOf() //пустая карта для хранения статусов
        val filesDir = filesDir
        for (file in filesDir.listFiles()) {
            if (file.name.startsWith("status_")) {
                val id = file.name.substringAfter("status_").substringBefore(".txt").toInt()
                val status = file.readText()
                statusMap[id] = status
            }
        }
        adapter.updateStatuses(statusMap)
    }
}
