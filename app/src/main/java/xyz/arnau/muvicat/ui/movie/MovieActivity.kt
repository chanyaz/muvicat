package xyz.arnau.muvicat.ui.movie

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.Snackbar
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.LinearLayoutManager
import com.google.android.youtube.player.YouTubeIntents
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.error_layout.view.*
import kotlinx.android.synthetic.main.movie_info.*
import xyz.arnau.muvicat.R
import xyz.arnau.muvicat.repository.model.*
import xyz.arnau.muvicat.ui.LocationAwareActivity
import xyz.arnau.muvicat.ui.SimpleDividerItemDecoration
import xyz.arnau.muvicat.utils.*
import xyz.arnau.muvicat.viewmodel.movie.MovieViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import javax.inject.Inject


class MovieActivity : LocationAwareActivity() {
    @Inject
    lateinit var movieViewModel: MovieViewModel
    @Inject
    lateinit var dateFormatter: DateFormatter
    @Inject
    lateinit var context: Context
    @Inject
    lateinit var infoAndShowingsAdapter: MovieShowingsAdapter

    private var hasLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)
        setContentView(R.layout.movie_info)
        setupToolbar(null)

        val movieId = intent.getLongExtra(MOVIE_ID, (-1).toLong())
        if (movieId == (-1).toLong())
            throw Exception("Missing movie identifier")
        else
            movieViewModel.setId(movieId)

        val showingId = intent.getLongExtra(SHOWING_ID, (-1).toLong())
        val cinemaId = intent.getLongExtra(CINEMA_ID, (-1).toLong())
        if (showingId != (-1).toLong()) {
            infoAndShowingsAdapter.showingId = showingId
            infoAndShowingsAdapter.expanded = false
        } else if (cinemaId != (-1).toLong()) {
            infoAndShowingsAdapter.cinemaId = cinemaId
            infoAndShowingsAdapter.expanded = false
        }

        movieInfoAndShowingsRecyclerView.adapter = infoAndShowingsAdapter
        movieInfoAndShowingsRecyclerView.layoutManager = LinearLayoutManager(this)
        movieInfoAndShowingsRecyclerView.addItemDecoration(SimpleDividerItemDecoration(context, 1))

        movieInfoToolbar.setOnClickListener {
            movieInfoToolbarLayout.setExpanded(true)
            movieInfoAndShowingsRecyclerView.scrollToPosition(0)
        }
    }

    override fun onStart() {
        super.onStart()

        movieViewModel.movie
            .observe(this, Observer { if (it != null) handleMovieDataState(it) })
        movieViewModel.showings
            .observe(this, Observer { if (it != null) handleShowingsDateState(it.status, it.data) })
    }

    private fun handleMovieDataState(movieRes: Resource<MovieWithCast>) {
        val movieWithCast = movieRes.data
        if (movieWithCast != null) {
            infoAndShowingsAdapter.castMembers = movieWithCast.castMembers.sortedBy { it.order }
            processMovie(movieWithCast.movie)
        }
        if (movieRes.status == Status.ERROR) {
            if (movieRes.data == null)
                finish()
            movieWithCast?.movie?.let { processMovie(it) }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun processMovie(movie: Movie) {
        setupToolbar(movie)

        GlideApp.with(context)
            .load("http://www.gencat.cat/llengua/cinema/${movie.posterUrl}")
            .centerCrop()
            .into(moviePoster)

        movieTitle.text = movie.title

        if (movie.year != null && movie.genres != null && movie.genres!!.isNotEmpty())
            movieYearGenreSeparator.setVisible()
        if (movie.runtime != null && movie.ageRating != null)
            movieRuntimeAgeRatingSeparator.setVisible()

        movieAgeRating.setVisibleText(movie.ageRating)
        movieYear.setVisibleText(movie.year?.toString())
        movieRuntime.setVisibleText(parseRuntime(movie.runtime))
        movieGenre.setVisibleText(parseGenres(movie.genres))

        movie.tmdbId?.let {
            ratingLayout.setVisible()
            movie.voteCount?.let {
                movie.voteAverage?.let {
                    if (movie.voteCount == 0) {
                        tmdbNoRatingText.setVisible()
                    } else {
                        tmdbRatingLayout.setVisible()
                        movieVoteAverage.text =
                                "${movie.voteAverage!!.div(2).toString1Decimal()}/5"
                        movieVoteCount.text = parseVoteCount(movie.voteCount!!)
                        movieVoteStars.rating = movie.voteAverage!!.div(2).toFloat()
                    }
                }
            }
        }

        movie.trailerUrl?.let { videoId ->
            playTrailer.setVisible()
            playTrailer.setOnClickListener { watchYoutubeVideo(this, videoId) }
        }

        if (movie.backdropUrl != null || movie.trailerUrl != null) {
            val url = if (movie.backdropUrl != null)
                "https://image.tmdb.org/t/p/w1280${movie.backdropUrl}"
            else
                "https://img.youtube.com/vi/${movie.trailerUrl}/maxresdefault.jpg"
            GlideApp.with(context)
                .load(url)
                .centerCrop()
                .into(movieBackdrop)
        }

        infoAndShowingsAdapter.movie = movie
        infoAndShowingsAdapter.notifyDataSetChanged()
    }

    private fun parseVoteCount(voteCount: Int): String {
        return when {
            voteCount == 1 -> return "1 ${getString(R.string.vote)}"
            voteCount < 1000 -> "$voteCount ${getString(R.string.votes)}"
            voteCount < 1000000 -> "${((voteCount / 100).toDouble() / 10).toString1Decimal()}m ${getString(
                R.string.votes
            )}"
            else -> "${(voteCount / 100000).toDouble() / 10}M ${getString(R.string.votes)}"
        }
    }

    private fun parseGenres(genres: List<String>?): String? {
        if (genres == null)
            return null

        return genres.toString().dropLast(1).drop(1)
    }

    private fun parseRuntime(runtime: Int?): String? {
        if (runtime == null)
            return null

        return when {
            runtime < 60 -> "$runtime min"
            runtime % 60 == 0 -> "${runtime / 60} h"
            else -> "${runtime / 60} h ${runtime % 60} min"
        }
    }

    private fun handleShowingsDateState(status: Status, showings: List<MovieShowing>?) {
        if (status == Status.SUCCESS) showings?.let {
            updateShowingsList(it, lastLocation)
        } else if (status == Status.ERROR) {
            if (showings != null && !showings.isEmpty()) {
                updateShowingsList(showings, lastLocation)
                Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.couldnt_update_data),
                    6000
                )
                    .show()
            }
        }
    }

    private fun setupToolbar(movie: Movie?) {
        setSupportActionBar(movieInfoToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        movieInfoToolbarCollapsing
            .setCollapsedTitleTypeface(
                ResourcesCompat.getFont(
                    context,
                    R.font.nunito_sans_extrabold
                )
            )
        movieInfoToolbar.setNavigationOnClickListener { onBackPressed() }


        movieInfoToolbarLayout.addOnOffsetChangedListener(object :
            AppBarLayout.OnOffsetChangedListener {
            var isShown = true
            var scrollRange = -1
            override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
                val backArrow =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_chevron_left_black, null)
                if (verticalOffset < -300) {
                    backArrow?.setColorFilter(Color.parseColor("#AF0000"), PorterDuff.Mode.SRC_ATOP)
                    supportActionBar!!.setHomeAsUpIndicator(backArrow)
                    movieInfoToolbarCollapsing.title = "Title"
                } else {
                    backArrow?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                    supportActionBar!!.setHomeAsUpIndicator(backArrow)
                }

                if (scrollRange == -1) {
                    scrollRange = movieInfoToolbarLayout.totalScrollRange
                }
                if (scrollRange + verticalOffset == 0) {
                    movieInfoToolbarCollapsing.title = movie?.title
                    isShown = true
                } else {
                    movieInfoToolbarCollapsing.title = " "
                    isShown = false
                }
            }
        })
    }

    private fun updateShowingsList(showings: List<MovieShowing>, lastLocation: Location?) {
        if (lastLocation != null) {
            setLocationToShowings(showings, lastLocation)
            hasLocation = true
        }
        infoAndShowingsAdapter.showings =
                showings.sortedWith(
                    compareBy<MovieShowing> { it.date }.thenBy(
                        nullsLast(),
                        { it.cinemaDistance })
                )
        infoAndShowingsAdapter.notifyDataSetChanged()
    }

    private fun setLocationToShowings(showings: List<MovieShowing>, location: Location) {
        showings.forEach {
            if (it.cinemaLatitude != null && it.cinemaLongitude != null) {
                it.cinemaDistance = LocationUtils.getDistance(
                    location,
                    it.cinemaLatitude!!,
                    it.cinemaLongitude!!
                )
            }
        }
    }

    override fun processLastLocation(location: Location) {
        if (::infoAndShowingsAdapter.isInitialized) {
            updateShowingsList(infoAndShowingsAdapter.showings, lastLocation)
        } else {
            hasLocation = false
        }
    }

    private fun watchYoutubeVideo(context: Context, id: String) {
        try {
            context.startActivity(
                YouTubeIntents.createPlayVideoIntentWithOptions(this, id, true, true)
            )
        } catch (ex: ActivityNotFoundException) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$id"))
            )
        }

    }


    companion object {
        private const val MOVIE_ID = "movie_id"
        private const val SHOWING_ID = "showing_id"
        private const val CINEMA_ID = "cinema_id"

        fun createIntent(
            context: Context,
            movieId: Long,
            showingId: Long? = null,
            cinemaId: Long? = null
        ): Intent {
            return Intent(context, MovieActivity::class.java).apply {
                putExtra(MOVIE_ID, movieId)
                showingId?.let { putExtra(SHOWING_ID, showingId) }
                cinemaId?.let { putExtra(CINEMA_ID, cinemaId) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    private fun Double.toString1Decimal(): String {
        val dfs = DecimalFormatSymbols(Locale.getDefault())
        dfs.decimalSeparator = ','
        val df = DecimalFormat("#.0", dfs)
        return df.format(this)
    }
}