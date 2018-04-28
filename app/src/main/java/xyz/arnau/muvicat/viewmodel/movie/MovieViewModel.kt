package xyz.arnau.muvicat.viewmodel.movie

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import xyz.arnau.muvicat.data.MovieRepository
import xyz.arnau.muvicat.data.model.Movie
import xyz.arnau.muvicat.data.model.Resource
import javax.inject.Inject

class MovieViewModel @Inject constructor(movieRepository: MovieRepository) : ViewModel() {
    private val movieId = MutableLiveData<Long>()

    val movie: LiveData<Resource<Movie>> = Transformations.switchMap(movieId) { id ->
        movieRepository.getMovie(id)
    }

    fun setId(id: Long) {
        movieId.value = id
    }
}