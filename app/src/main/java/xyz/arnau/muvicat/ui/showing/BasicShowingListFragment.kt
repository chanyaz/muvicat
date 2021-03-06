package xyz.arnau.muvicat.ui.showing

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ethanhua.skeleton.RecyclerViewSkeletonScreen
import com.ethanhua.skeleton.Skeleton
import kotlinx.android.synthetic.main.error_layout.*
import kotlinx.android.synthetic.main.showing_list.*
import xyz.arnau.muvicat.R
import xyz.arnau.muvicat.repository.model.Resource
import xyz.arnau.muvicat.repository.model.Status
import xyz.arnau.muvicat.di.Injectable
import xyz.arnau.muvicat.ui.ListFragment
import xyz.arnau.muvicat.ui.utils.QueuedSnack
import xyz.arnau.muvicat.ui.utils.SimpleDividerItemDecoration
import xyz.arnau.muvicat.ui.utils.SnackQueue
import xyz.arnau.muvicat.utils.setGone
import xyz.arnau.muvicat.utils.setVisible
import javax.inject.Inject

abstract class BasicShowingListFragment<T> : ListFragment(), Injectable {
    private lateinit var skeleton: RecyclerViewSkeletonScreen
    @Inject
    lateinit var snackQueue: SnackQueue

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupSkeletonScreen()
        getRecyclerView().addItemDecoration(
            SimpleDividerItemDecoration(
                context!!
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.showings_fragment, container, false)
    }

    override fun onStart() {
        super.onStart()
        getShowingsLiveData().observe(this,
            Observer<Resource<List<T>>> {
                if (it != null) handleDateState(it.status, it.data)
            })
    }

    internal abstract fun getShowingsLiveData(): LiveData<Resource<List<T>>>

    private fun handleDateState(status: Status, data: List<T>?) {
        if (status == Status.SUCCESS) data?.let {
            handleShowingsUpdate(data)
            skeleton.hide()
            restoreRecyclerViewState()
            if (data.isEmpty()) {
                showingsRecyclerView.setGone()
                errorMessage.setVisible()
            }
        } else if (status == Status.ERROR) {
            if (data != null && !data.isEmpty()) {
                handleShowingsUpdate(data)
                skeleton.hide()
                snackQueue.enqueueSnack(
                    QueuedSnack(
                        activity!!,
                        getString(R.string.couldnt_update_data),
                        8000
                    ), SnackQueue.COULDNT_UPDATE)
            } else {
                skeleton.hide()
                showingsRecyclerView.setGone()
                errorMessage.setVisible()
            }
        }
    }

    abstract fun handleShowingsUpdate(data: List<T>)

    override fun getRecyclerView(): RecyclerView = showingsRecyclerView

    override fun getRecyclerViewLayoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(context)

    private fun setupSkeletonScreen() {
        skeleton = Skeleton.bind(showingsRecyclerView)
            .adapter(getRecyclerViewAdapter())
            .count(8)
            .color(R.color.skeleton_shimmer)
            .load(R.layout.showing_item_skeleton)
            .show()
    }
}