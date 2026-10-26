package com.kovaleff.cheapeats.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kovaleff.cheapeats.data.model.CategoryDto
import com.kovaleff.cheapeats.data.model.CityDto
import com.kovaleff.cheapeats.data.model.ReviewDto
import com.kovaleff.cheapeats.data.repository.CheapEatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val cities: List<CityDto> = emptyList(),
    val selectedCity: CityDto? = null,
    val categories: List<CategoryDto> = emptyList(),
    val reviews: List<ReviewDto> = emptyList(),
)

class HomeViewModel(
    private val repository: CheapEatsRepository = CheapEatsRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun selectCity(city: CityDto) {
        _state.update { it.copy(selectedCity = city) }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val cities = repository.getCities()
                val selected = _state.value.selectedCity
                    ?: cities.firstOrNull { it.isDefault == 1 }
                    ?: cities.firstOrNull()
                val categories = repository.getCategories()
                    .map { it.copy(image = repository.absoluteUrl(it.image)) }
                val reviews = try {
                    repository.getLatestReviews()
                } catch (_: Exception) {
                    emptyList()
                }.map { review ->
                    review.copy(user = review.user?.copy(photo200 = repository.absoluteUrl(review.user?.photo200)))
                }
                _state.update {
                    it.copy(
                        loading = false,
                        cities = cities,
                        selectedCity = selected,
                        categories = categories,
                        reviews = reviews,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Ошибка загрузки")
                }
            }
        }
    }
}
