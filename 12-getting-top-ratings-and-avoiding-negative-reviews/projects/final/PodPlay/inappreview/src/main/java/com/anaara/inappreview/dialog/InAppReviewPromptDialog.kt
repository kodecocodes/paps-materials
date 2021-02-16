/*
 *   Copyright (c) 2020 Razeware LLC
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 *   distribute, sublicense, create a derivative work, and/or sell copies of the
 *   Software in any work that is designed, intended, or marketed for pedagogical or
 *   instructional purposes related to programming, coding, application development,
 *   or information technology.  Permission for such use, copying, modification,
 *   merger, publication, distribution, sublicensing, creation of derivative works,
 *   or sale is expressly withheld.
 *
 *   This project and source code may use libraries or frameworks that are
 *   released under various Open-Source licenses. Use of those libraries and
 *   frameworks are governed by their own individual licenses.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 */

package com.anaara.inappreview.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.anaara.inappreview.R
import dagger.hilt.android.AndroidEntryPoint
import com.anaara.inappreview.databinding.FragmentInAppReviewPromptBinding

/**
 * Shows a dialog that asks the user if they want to review the app.
 *
 * This dialog is shown only if the user hasn't previously rated the app, hasn't asked to never
 * rate the app or if they asked to rate it later and enough time passed (a week).
 * */
@AndroidEntryPoint
class InAppReviewPromptDialog : DialogFragment() {

  private var binding: FragmentInAppReviewPromptBinding? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    binding = FragmentInAppReviewPromptBinding.inflate(inflater, container, false)

    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initListeners()
    dialog?.setCanceledOnTouchOutside(false)
  }

  private fun initListeners() {
    val binding = binding ?: return

    binding.leaveReview.setOnClickListener { onLeaveReviewTapped() }
    binding.reviewLater.setOnClickListener { onRateLaterTapped() }
  }

  private fun onLeaveReviewTapped() {
    // TODO
    dismissAllowingStateLoss()
  }

  private fun onRateLaterTapped() {
    // TODO
    dismissAllowingStateLoss()
  }

  /**
   * Styles the dialog to have a transparent background and window insets.
   */
  override fun onStart() {
    super.onStart()
    initStyle()
  }

  private fun initStyle() {
    val back = ColorDrawable(Color.TRANSPARENT)
    dialog?.window?.setBackgroundDrawable(back)

    dialog?.window?.setLayout(
        resources.getDimensionPixelSize(R.dimen.ratePromptWidth),
        resources.getDimensionPixelSize(R.dimen.ratePromptHeight)
    )

    binding?.progressBar?.setImageResource(R.drawable.progress_white)
  }

}