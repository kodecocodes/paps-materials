/*
 * Copyright (c) 2021 Razeware LLC
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

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.raywenderlich.podplay.BuildConfig
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.adapter.PodcastListAdapter.PodcastListAdapterListener
import com.raywenderlich.podplay.db.PodPlayDatabase
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.model.LOCATION_PERMISSION_REQUEST_CODE
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.ui.PodcastDetailsFragment.OnPodcastDetailsListener
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.viewmodel.PodcastViewModel.EpisodeViewData
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import com.raywenderlich.podplay.worker.EpisodeUpdateWorker
import kotlinx.android.synthetic.main.activity_podcast.*
import java.util.concurrent.TimeUnit

class PodcastActivity :
    AppCompatActivity(),
    PodcastListAdapterListener,
    OnPodcastDetailsListener {

  private val searchViewModel by viewModels<SearchViewModel>()
  private val podcastViewModel by viewModels<PodcastViewModel>()
  private lateinit var podcastListAdapter: PodcastListAdapter
  private lateinit var searchMenuItem: MenuItem
  private lateinit var fusedLocationClient: FusedLocationProviderClient // Location Services Client

  private var searchTerm = ""
  private val DEFAULT_COUNTRY = "US" // search United State, by default


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

  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<String>,
      grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
      LOCATION_PERMISSION_REQUEST_CODE -> when {
        grantResults.isEmpty() ->
          // If user interaction was interrupted, the permission request
          // is cancelled and you receive empty arrays.
          Log.d("PodcastActivity", "User interaction was cancelled.")

        grantResults[0] == PackageManager.PERMISSION_GRANTED ->
          // Permission was granted.
          searchUsingLocation(searchTerm)

        else -> {
          // Permission denied.
          Snackbar.make(
                  podcastDetailsContainer,
                  R.string.permission_denied_explanation,
                  Snackbar.LENGTH_LONG
          )
                  .setAction(R.string.settings) {
                    // Build intent that displays the App settings screen.
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID,
                            null
                    )
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                  }
                  .show()
          performSearch(searchTerm, DEFAULT_COUNTRY)
        }
      }
    }
  }

  private fun checkLocationPermissionAndSearch(term: String) {
    searchTerm = term
    when {
      // 1
      checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
        // Permission granted. Proceed to use the location.
        searchUsingLocation(term)
      }
      // 2
      // If the user denied a previous request, but didn't check "Don't ask again", provide
      // additional rationale.
      shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
        // In an educational UI, explain to the user why your app requires this
        // permission for a specific feature to behave as expected. In this UI,
        // if possible, include a "cancel" or "no thanks" button that allows the user to
        // continue using your app without granting the permission.
        Snackbar.make(
                podcastDetailsContainer,
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
        )
                .setAction(R.string.ok) {
                  // Request permission
                  ActivityCompat.requestPermissions(
                          this,
                          arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                          LOCATION_PERMISSION_REQUEST_CODE
                  )
                }
                .show()
      }
      else -> {
        // 3
        // Display the system permissions dialog when necessary
        Log.d("PodcastActivity", "Request location permission")
        ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
        )
      }
    }
  }

  /**
   * Sets the user's country code based on fused location service
   */
  private fun searchUsingLocation(searchTerm: String) {
    //Create Location Services Client
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    // 1
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      return
    }
    // 2
    fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
              // Got last known location. In some rare situations this can be null.
              location?.let {
                // 3
                val geoCoder = Geocoder(this)
                val addresses = geoCoder.getFromLocation(location.latitude,location.longitude, 1)
                val searchCountry = addresses[0].countryCode
                performSearch(searchTerm, searchCountry)
              } ?: run {
                performSearch(searchTerm, DEFAULT_COUNTRY)
              }
            }.addOnFailureListener {
                performSearch(searchTerm, DEFAULT_COUNTRY)
            }
  }

  private fun performSearch(searchTerm: String, searchCountry: String) {
    showProgressBar()
    searchViewModel.searchPodcasts(searchTerm, country = searchCountry) { results ->
      hideProgressBar()
      toolbar.title = searchTerm
      podcastListAdapter.setSearchData(results)
    }
  }

  private fun handleIntent(intent: Intent) {
    if (Intent.ACTION_SEARCH == intent.action) {
      val query = intent.getStringExtra(SearchManager.QUERY) ?: return
      checkLocationPermissionAndSearch(query)
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
  }
}
