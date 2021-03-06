/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2017. Dmytro Karataiev
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.adkdevelopment.moviesdb.feat_series.presenters

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.adkdevelopment.moviesdb.App
import com.adkdevelopment.moviesdb.R
import com.adkdevelopment.moviesdb.data.model.TvResults
import com.adkdevelopment.moviesdb.feat_series.contracts.SeriesContract
import com.adkdevelopment.moviesdb.ui.base.BaseMvpPresenter
import com.adkdevelopment.moviesdb.utils.Utility
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

/**
 * Presenter for the SeriesFragment.
 * Created by Dmytro Karataiev on 10/22/16.
 */
class SeriesPresenter(private val mContext: Context) :
        BaseMvpPresenter<SeriesContract.View>(),
        SeriesContract.Presenter,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private var mSubscription: CompositeSubscription
    private var isUpdating: Boolean = false

    init {
        mSubscription = CompositeSubscription()
    }

    override fun requestData(page: Int) {
        checkViewAttached()
        if (!isUpdating) {
            isUpdating = true
            mvpView.showProgress()

            val sort = Utility.getSeriesSort(mContext)

            if (!mSubscription.isUnsubscribed && page == 1) {
                mSubscription.unsubscribe()
                mSubscription = CompositeSubscription()
            }

            mSubscription.add(App.getApiManager().moviesService.getSeries(sort, page)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : Subscriber<TvResults>() {
                        override fun onCompleted() {
                            mvpView.showProgress()
                        }

                        override fun onError(e: Throwable) {
                            Log.e(TAG, "onError: ", e)
                            mvpView.showProgress()
                            mvpView.showError()
                        }

                        override fun onNext(tvResults: TvResults) {
                            if (tvResults.results != null && tvResults.results.size > 0) {
                                mvpView.showData(tvResults.results, page)
                            } else if (page == 1) {
                                mvpView.showEmpty()
                            }
                            isUpdating = false
                        }
                    }))
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == mContext.getString(R.string.pref_sort_series_key)) {
            requestData(1)
        }
    }

    override fun attachView(mvpView: SeriesContract.View) {
        super.attachView(mvpView)
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun detachView() {
        super.detachView()
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .unregisterOnSharedPreferenceChangeListener(this)
        if (!mSubscription.isUnsubscribed) {
            mSubscription.unsubscribe()
        }
    }

    companion object {
        private val TAG = SeriesPresenter::class.java.simpleName
    }
}
