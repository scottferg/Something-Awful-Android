/**
 * *****************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 *
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 *
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *****************************************************************************
 */

package com.ferg.awfulapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.Toolbar
import android.view.KeyEvent

import com.ferg.awfulapp.announcements.AnnouncementsManager
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.dialog.Changelog
import com.ferg.awfulapp.messages.PmManager
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.Keys
import com.ferg.awfulapp.sync.SyncManager

import java.util.Locale

import timber.log.Timber


class ForumsIndexActivity :
        AwfulImmersionActivity(),
        PmManager.Listener,
        AnnouncementsManager.AnnouncementListener,
        PagerCallbacks {

    private lateinit var forumsPager: ForumsPagerController
    private lateinit var toolbar: Toolbar
    private lateinit var navigationDrawer: NavigationDrawer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forum_index_activity)

        val viewPager: SwipeLockViewPager = findViewById(R.id.forum_index_pager)
        forumsPager = ForumsPagerController(viewPager, mPrefs, this, this, savedInstanceState)

        toolbar = findViewById(R.id.awful_toolbar)
        setSupportActionBar(toolbar)
        setUpActionBar()

        navigationDrawer = NavigationDrawer(this, toolbar, mPrefs)
        updateNavigationDrawer()

        PmManager.registerListener(this)
        AnnouncementsManager.getInstance().registerListener(this)

        // only handle the Activity's intent when it's first created, or that navigation event will be fired whenever the activity is restored
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
        showChangelogIfRequired()
    }

    override fun onNewPm(messageUrl: String, sender: String, unreadCount: Int) {
        val showPmEvent = NavigationEvent.ShowPrivateMessages(Uri.parse(messageUrl))
        runOnUiThread {
            val message = "Private message from %s\n(%d unread)"
            makeSnackbar(String.format(Locale.getDefault(), message, sender, unreadCount), showPmEvent)
        }
    }

    override fun onAnnouncementsUpdated(newCount: Int, oldUnread: Int, oldRead: Int, isFirstUpdate: Boolean) {
        // only show one of 'new announcements' or 'unread announcements', ignoring read ones
        // (only notify about unread for the first update after opening the app, to remind the user)
        val areNewAnnouncements = newCount > 0
        if (isFirstUpdate || areNewAnnouncements) {
            val res = resources
            if (areNewAnnouncements) {
                makeSnackbar(res.getQuantityString(R.plurals.numberOfNewAnnouncements, newCount, newCount))
            } else if (oldUnread > 0) {
                makeSnackbar(res.getQuantityString(R.plurals.numberOfOldUnreadAnnouncements, oldUnread, oldUnread))
            }
        }
    }

    private fun makeSnackbar(message: String, event: NavigationEvent = NavigationEvent.Announcements) {
        Snackbar.make(toolbar, message, Snackbar.LENGTH_LONG)
                .setDuration(3000)
                .setAction("View") { navigate(event) }
                .show()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        navigationDrawer.drawerToggle.syncState()
    }


    private fun updateNavigationDrawer() {
        // TODO: 17/01/2018 what is this actually meant to display? The current thread and the forum (including Bookmarks) that it's in? Or the current state of each page?
        // display details for the currently open thread - if there isn't one, show the current forum instead
        val threadFragment = forumsPager.getThreadDisplayFragment()
        val forumFragment = forumsPager.getForumDisplayFragment()
        if (threadFragment != null) {
            val threadId = threadFragment.threadId
            val parentForumId = threadFragment.parentForumId
            navigationDrawer.setCurrentForumAndThread(parentForumId, threadId)
        } else if (forumFragment != null) {
            val forumId = forumFragment.forumId
            navigationDrawer.setCurrentForumAndThread(forumId, null)
        }
    }


    private fun updateTitle() {
        super.setActionbarTitle(forumsPager.getVisibleFragmentTitle())
    }

    override fun setActionbarTitle(title: String) {
        throw RuntimeException("Don't set this directly!")
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment events
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Notify the activity that something about a pager fragment has changed, so it can update appropriately.
     *
     *
     * This could be extended by passing the fragment in if necessary, or adding methods for each
     * page (onThreadChanged etc) - but right now this is all we need.
     */
    fun onPageContentChanged() {
        updateNavigationDrawer()
        updateTitle()
    }


    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////





    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }


    /**
     * Parse and process an intent, e.g. to handle a navigation event.
     */
    private fun handleIntent(intent: Intent) {
        val parsed = NavigationEvent.parse(intent)
        Timber.i("Parsed intent as %s", parsed.toString())
        navigate(parsed)
    }


    override fun handleNavigation(event: NavigationEvent): Boolean {
        // TODO: when this is all Kotlins, add an optional private "from intent" param that defaults to false - set it true when we're handling an event that opened this activity, and throw when it isn't handled, or we'll just keep reopening the activity
        return when(event) {
            is NavigationEvent.MainActivity -> true
            is NavigationEvent.ForumIndex -> {
                forumsPager.currentPagerItem = Pages.ForumIndex
                true
            }
            is NavigationEvent.Bookmarks, is NavigationEvent.Forum, is NavigationEvent.Thread, is NavigationEvent.Url -> {
                forumsPager.navigate(event)
                true
            }
            is NavigationEvent.ReAuthenticate -> {
                Authentication.reAuthenticate(this)
                true
            }
            else -> false
        }
    }


    private fun showChangelogIfRequired() {
        val versionCode = BuildConfig.VERSION_CODE
        val lastVersionCode = mPrefs.lastVersionSeen

        if (lastVersionCode != versionCode) {
            Timber.i("App version changed from %d to %d - showing changelog", lastVersionCode, versionCode)
            Changelog.showDialog(this, 1)
            mPrefs.setPreference(Keys.LAST_VERSION_SEEN, versionCode)
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        forumsPager.onSaveInstanceState(outState)
    }


    override fun onPageChanged(page: Pages, pageFragment: AwfulFragment) {
        // I don't know if #isAdded is necessary after calling #instantiateItem (instead of #getItem
        // which just creates a new fragment object), but I'm trying to fix a bug I can't reproduce
        // where these fragment methods crash because they have no activity yet
        Timber.i("onPageChanged: page %s fragment %s", page, pageFragment)
        updateTitle()
        if (pageFragment.isAdded) {
            setProgress(pageFragment.progressPercent)
        }
    }


    override fun onBackPressed() {
        // in order of precedence: close the nav drawer, tell the current fragment to go back, tell the pager to go back
        if (navigationDrawer.close() || forumsPager.onBackPressed()) return
        super.onBackPressed()
    }


    override fun onActivityResult(request: Int, result: Int, intent: Intent?) {
        super.onActivityResult(request, result, intent)
        if (request == Constants.LOGIN_ACTIVITY_REQUEST && result == Activity.RESULT_OK) {
            Timber.i("Result from login activity: successful login - calling sync")
            SyncManager.sync(this)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val pagerItem = forumsPager.getCurrentFragment()
        return if (mPrefs.volumeScroll && pagerItem?.attemptVolumeScroll(event) == true) {
            true
        } else super.dispatchKeyEvent(event)
    }

    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        super.onPreferenceChange(prefs, key)
        forumsPager.onPreferenceChange(prefs)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Timber.e("onConfigurationChanged()")
        forumsPager.onConfigurationChange(mPrefs)
        val currentFragment = forumsPager.getCurrentFragment()
        currentFragment?.onConfigurationChanged(newConfig)
        navigationDrawer.drawerToggle.onConfigurationChanged(newConfig)
    }

    fun preventSwipe() {
        forumsPager.setSwipeEnabled(false)
    }

    fun allowSwipe() {
        runOnUiThread { forumsPager.setSwipeEnabled(true) }
    }
}
