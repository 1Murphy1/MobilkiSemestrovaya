package com.example.mooddiary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MoodViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MoodRepository
    val allMoods: LiveData<List<MoodEntry>>

    init {
        //DAO для работы с бд и создание репозиторий
        val moodDao = MoodDatabase.getDatabase(application).moodDao()
        repository = MoodRepository(moodDao)

        //список всех заметок из репозитория
        allMoods = repository.allMoods
    }
    fun insert(moodEntry: MoodEntry) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(moodEntry)
    }
    fun update(moodEntry: MoodEntry) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(moodEntry)
    }
    fun delete(moodEntry: MoodEntry) = viewModelScope.launch(Dispatchers.IO){
        repository.delete(moodEntry)
    }
}
