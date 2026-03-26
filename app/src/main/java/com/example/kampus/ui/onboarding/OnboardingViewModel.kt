package com.example.kampus.ui.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingViewModel : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    fun onPageChanged(page: Int) {
        _currentPage.value = page
    }

    fun isLastPage(totalPages: Int): Boolean =
        _currentPage.value == totalPages - 1

    fun nextPage(totalPages: Int) {
        if (_currentPage.value < totalPages - 1) {
            _currentPage.value++
        }
    }

    fun skipToLast(totalPages: Int) {
        _currentPage.value = totalPages - 1
    }
}