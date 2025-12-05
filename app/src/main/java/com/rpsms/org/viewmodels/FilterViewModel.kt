package com.rpsms.org.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rpsms.org.database.FilterDao
import com.rpsms.org.models.FilterGroup
import kotlinx.coroutines.launch

class FilterViewModel(private val filterDao: FilterDao) : ViewModel() {
    
    val allFilters: LiveData<List<FilterGroup>> = filterDao.getAllFilters().asLiveData()
    
    fun insertFilter(filter: FilterGroup) = viewModelScope.launch {
        filterDao.insert(filter)
    }
    
    fun updateFilter(filter: FilterGroup) = viewModelScope.launch {
        filterDao.update(filter)
    }
    
    fun deleteFilter(filter: FilterGroup) = viewModelScope.launch {
        filterDao.delete(filter)
    }
    
    fun getFilterById(id: Long): LiveData<FilterGroup?> {
        // This would require a separate query in DAO
        // For now, we filter from all filters
        return allFilters.map { filters ->
            filters.find { it.id == id }
        }.asLiveData()
    }
    
    @Suppress("UNCHECKED_CAST")
    class Factory(private val filterDao: FilterDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FilterViewModel::class.java)) {
                return FilterViewModel(filterDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
