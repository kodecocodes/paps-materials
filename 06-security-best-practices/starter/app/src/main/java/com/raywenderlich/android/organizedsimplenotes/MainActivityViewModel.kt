package com.raywenderlich.android.organizedsimplenotes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

private const val ENCRYPTED_PREFS = "ENCRYPTED_PREFS"
private const val ENCRYPTED_PREFS_ENCRYPTION_KEY = "ENCRYPTED_PREFS_ENCRYPTION_KEY"

class MainActivityViewModel(application: Application) : AndroidViewModel(application)  {

  val snackbar: LiveData<String?>
    get() = _snackbar

  private val _snackbar = MutableLiveData<String?>()

  private val context by lazy { getApplication<Application>().applicationContext }

  fun onSnackbarShown() {
    _snackbar.value = null
  }

  fun getEncryptionKey(): String? {
    // TODO
    return null
  }

  fun setEncryptionKey(current: String?, new: String?) {
    if (current != getEncryptionKey()) {
      _snackbar.value = context.getString(R.string.error_current_encryption_key_incorrect)
      return
    }
  }

}