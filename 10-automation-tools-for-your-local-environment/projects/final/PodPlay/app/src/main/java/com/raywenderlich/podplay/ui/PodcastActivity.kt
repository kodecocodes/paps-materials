/*
 * Copyright (c) 2020 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.podplay.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.adapter.PodcastListAdapter.PodcastListAdapterListener
import com.raywenderlich.podplay.db.PodPlayDatabase
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.ui.PodcastDetailsFragment.OnPodcastDetailsListener
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.viewmodel.PodcastViewModel.EpisodeViewData
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import com.raywenderlich.podplay.worker.EpisodeUpdateWorker
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.activity_podcast.podcastRecyclerView
import kotlinx.android.synthetic.main.activity_podcast.progressBar
import kotlinx.android.synthetic.main.activity_podcast.toolbar

class PodcastActivity :
        AppCompatActivity(),
        PodcastListAdapterListener,
        OnPodcastDetailsListener {

  private val searchViewModel by viewModels<SearchViewModel>()
  private val podcastViewModel by viewModels<PodcastViewModel>()
  private lateinit var podcastListAdapter: PodcastListAdapter
  private lateinit var searchMenuItem: MenuItem
  private lateinit var downloadMenuItem: MenuItem
  private lateinit var notesMenuItem: MenuItem
  private var areNotesEnabled = false
  private var isInstallTimeModuleAvailable = true
  private lateinit var settingsMenuItem: MenuItem

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_podcast)
    setupToolbar()
    setupViewModels()
    updateControls()
    setupPodcastListView()
    handleIntent(intent)
    addBackStackListener()
    scheduleJobs()
  }

  override fun onSubscribe() {
    podcastViewModel.saveActivePodcast()
    supportFragmentManager.popBackStack()
  }

  override fun onUnsubscribe() {
    podcastViewModel.deleteActivePodcast()
    supportFragmentManager.popBackStack()
  }

  override fun onShowEpisodePlayer(episodeViewData: EpisodeViewData) {
    podcastViewModel.activeEpisodeViewData = episodeViewData
    showPlayerFragment()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.menu_search, menu)

    searchMenuItem = menu.findItem(R.id.search_item)
    settingsMenuItem = menu.findItem(R.id.install_time_delivery_button)

    val searchView = searchMenuItem.actionView as SearchView

    searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
      override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
        return true
      }
      override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
        showSubscribedPodcasts()
        return true
      }
    })

    val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
    searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

    if (supportFragmentManager.backStackEntryCount > 0) {
      podcastRecyclerView.visibility = View.INVISIBLE
    }

    if (podcastRecyclerView.visibility == View.INVISIBLE) {
      searchMenuItem.isVisible = false
    }

    if (isInstallTimeModuleAvailable) {
      settingsMenuItem = menu.findItem(R.id.install_time_delivery_button)
    }

    try {
      Class.forName(SETTINGS_CLASS_NAME)
      isInstallTimeModuleAvailable = true
      settingsMenuItem.isVisible = true
    } catch (e: Exception) {
      isInstallTimeModuleAvailable = false
      Log.d(TAG_ACTIVITY, "Couldn't start SettingsActivity, the class doesn't exist")
    }

    if (isInstallTimeModuleAvailable) {
      settingsMenuItem.setOnMenuItemClickListener {
        val intent = Intent().setClassName(this, SETTINGS_CLASS_NAME)
        startActivity(intent)
        true
      }
    }

    downloadMenuItem = menu.findItem(R.id.download_on_demand_module_item)

    downloadMenuItem.setOnMenuItemClickListener {
      downloadModule()
    }

    notesMenuItem = menu.findItem(R.id.write_notes_item)

    visibilityOfNotesFeature()

    return true
  }

  private fun visibilityOfNotesFeature(): Boolean {
    notesMenuItem.isVisible = areNotesEnabled
    return true
  }

  private fun downloadModule(): Boolean {
    // 1
    val splitInstallManager = SplitInstallManagerFactory.create(applicationContext)

    //2
    val request = SplitInstallRequest
            .newBuilder()
            .addModule("onDemandDeliveryExample")
            .build()

    //3
    splitInstallManager
            .startInstall(request)
            .addOnSuccessListener { sessionId ->
              Toast.makeText(
                      applicationContext,
                      "Module installed successfully with sessionId $sessionId",
                      Toast.LENGTH_LONG
              ).show()
            }
            .addOnFailureListener { exception ->
              Toast.makeText(
                      applicationContext,
                      "Module not installed with exception $exception",
                      Toast.LENGTH_LONG
              ).show()
            }

    return true
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {

    val feedUrl = podcastSummaryViewData.feedUrl ?: return

    showProgressBar()

    podcastViewModel.getPodcast(podcastSummaryViewData) {

      hideProgressBar()

      if (it != null) {
        showDetailsFragment()
      } else {
        showError("Error loading feed $feedUrl")
      }
    }
  }

  private fun scheduleJobs() {

    val constraints: Constraints = Constraints.Builder().apply {
      setRequiredNetworkType(NetworkType.CONNECTED)
      setRequiresCharging(true)
    }.build()

    val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(
            1,
            TimeUnit.HOURS
    ).setConstraints(constraints).build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TAG_EPISODE_UPDATE_JOB,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
    )
  }

  private fun showSubscribedPodcasts() {
    val podcasts = podcastViewModel.getPodcasts()?.value

    if (podcasts != null) {
      toolbar.title = getString(R.string.subscribed_podcasts)
      podcastListAdapter.setSearchData(podcasts)
    }
  }

  private fun performSearch(term: String) {
    showProgressBar()
    searchViewModel.searchPodcasts(term) { results ->
      hideProgressBar()
      toolbar.title = term
      podcastListAdapter.setSearchData(results)
    }
  }

  private fun handleIntent(intent: Intent) {
    if (Intent.ACTION_SEARCH == intent.action) {
      val query = intent.getStringExtra(SearchManager.QUERY) ?: return
      performSearch(query)
    }
    val podcastFeedUrl = intent.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)
    if (podcastFeedUrl != null) {
      podcastViewModel.setActivePodcast(podcastFeedUrl) {
        it?.let { podcastSummaryView -> onShowDetails(podcastSummaryView) }
      }
    }
  }

  private fun setupToolbar() {
    setSupportActionBar(toolbar)
  }

  private fun setupViewModels() {
    val service = ItunesService.instance
    searchViewModel.iTunesRepo = ItunesRepo(service)
    val rssService = FeedService.instance
    val db = PodPlayDatabase.getInstance(this)
    val podcastDao = db.podcastDao()
    podcastViewModel.podcastRepo = PodcastRepo(rssService, podcastDao)
  }

  private fun setupPodcastListView() {
    podcastViewModel.getPodcasts()?.observe(
            this,
            {
              if (it != null) {
                showSubscribedPodcasts()
              }
            }
    )
  }

  private fun addBackStackListener() {
    supportFragmentManager.addOnBackStackChangedListener {
      if (supportFragmentManager.backStackEntryCount == 0) {
        podcastRecyclerView.visibility = View.VISIBLE
      }
    }
  }

  private fun updateControls() {
    podcastRecyclerView.setHasFixedSize(true)

    val layoutManager = LinearLayoutManager(this)
    podcastRecyclerView.layoutManager = layoutManager

    val dividerItemDecoration = DividerItemDecoration(
            podcastRecyclerView.context,
            layoutManager.orientation
    )
    podcastRecyclerView.addItemDecoration(dividerItemDecoration)

    podcastListAdapter = PodcastListAdapter(null, this, this)
    podcastRecyclerView.adapter = podcastListAdapter
  }

  private fun showDetailsFragment() {
    val podcastDetailsFragment = createPodcastDetailsFragment()

    supportFragmentManager.beginTransaction().add(
            R.id.podcastDetailsContainer,
            podcastDetailsFragment, TAG_DETAILS_FRAGMENT
    ).addToBackStack("DetailsFragment").commit()
    podcastRecyclerView.visibility = View.INVISIBLE
    searchMenuItem.isVisible = false
  }

  private fun showPlayerFragment() {
    val episodePlayerFragment = createEpisodePlayerFragment()

    supportFragmentManager.beginTransaction().replace(
            R.id.podcastDetailsContainer,
            episodePlayerFragment,
            TAG_PLAYER_FRAGMENT
    ).addToBackStack("PlayerFragment").commit()
    podcastRecyclerView.visibility = View.INVISIBLE
    searchMenuItem.isVisible = false
  }

  private fun createEpisodePlayerFragment(): EpisodePlayerFragment {

    var episodePlayerFragment = supportFragmentManager.findFragmentByTag(TAG_PLAYER_FRAGMENT) as
            EpisodePlayerFragment?

    if (episodePlayerFragment == null) {
      episodePlayerFragment = EpisodePlayerFragment.newInstance()
    }
    return episodePlayerFragment
  }

  private fun createPodcastDetailsFragment(): PodcastDetailsFragment {
    var podcastDetailsFragment = supportFragmentManager.findFragmentByTag(TAG_DETAILS_FRAGMENT) as
            PodcastDetailsFragment?

    if (podcastDetailsFragment == null) {
      podcastDetailsFragment = PodcastDetailsFragment.newInstance()
    }

    return podcastDetailsFragment
  }

  private fun showProgressBar() {
    progressBar.visibility = View.VISIBLE
  }

  private fun hideProgressBar() {
    progressBar.visibility = View.INVISIBLE
  }

  private fun showError(message: String) {
    AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
  }

  companion object {
    private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
    private const val TAG_EPISODE_UPDATE_JOB = "com.raywenderlich.podplay.episodes"
    private const val TAG_PLAYER_FRAGMENT = "PlayerFragment"
    private const val TAG_ACTIVITY = "PodcastActivity"
    private const val SETTINGS_CLASS_NAME = "com.raywenderlich.installtimedeliveryexample.SettingsActivity"
  }
}
