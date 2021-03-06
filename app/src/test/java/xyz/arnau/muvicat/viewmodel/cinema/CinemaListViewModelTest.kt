package xyz.arnau.muvicat.viewmodel.cinema

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import xyz.arnau.muvicat.repository.CinemaRepository
import xyz.arnau.muvicat.repository.model.Cinema
import xyz.arnau.muvicat.repository.model.Resource
import xyz.arnau.muvicat.repository.model.Status
import xyz.arnau.muvicat.repository.test.CinemaFactory
import xyz.arnau.muvicat.utils.getValueBlocking

@RunWith(JUnit4::class)
class CinemaListViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val cinemaRepository = mock(CinemaRepository::class.java)
    private lateinit var cinemaListViewModel: CinemaListViewModel
    private val cinemasLiveData = MutableLiveData<Resource<List<Cinema>>>()

    @Before
    fun setUp() {
        `when`(cinemaRepository.getCinemas()).thenReturn(cinemasLiveData)
        cinemaListViewModel = CinemaListViewModel(cinemaRepository)
    }

    @Test
    fun getCinemasReturnsLiveData() {
        val cinemas = CinemaFactory.makeCinemaList(5)
        cinemasLiveData.postValue(Resource.success(cinemas))

        val result = cinemaListViewModel.cinemas.getValueBlocking()
        assertEquals(Status.SUCCESS, result!!.status)
        assertEquals(null, result.message)
        assertEquals(cinemas, result.data)
    }
}