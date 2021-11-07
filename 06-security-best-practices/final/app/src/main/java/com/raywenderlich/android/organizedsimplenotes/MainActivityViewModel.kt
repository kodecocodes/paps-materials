package com.raywenderlich.android.organizedsimplenotes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

private const val ENCRYPTED_PREFS = "ENCRYPTED_PREFS"
private const val ENCRYPTED_PREFS_ENCRYPTION_KEY = "ENCRYPTED_PREFS_ENCRYPTION_KEY"

class MainActivityViewModel(application: Application) : AndroidViewModel(application)  {

  val snackbar: LiveData<String?>
    get() = _snackbar

  private val _snackbar = MutableLiveData<String?>()

  private val context by lazy { getApplication<Application>().applicationContext }

  private val sharedPreferences by lazy {
    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    val keyEncryptionScheme = EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
    val valueEncryptionScheme = EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM

    EncryptedSharedPreferences.create(
      ENCRYPTED_PREFS,
      masterKeyAlias,
      context,
      keyEncryptionScheme,
      valueEncryptionScheme
    )
  }

  fun onSnackbarShown() {
    _snackbar.value = null
  }

  fun getEncryptionKey(): String? {
    return sharedPreferences.getString(
      ENCRYPTED_PREFS_ENCRYPTION_KEY,
      null
    )
  }

  fun setEncryptionKey(current: String?, new: String?) {
    if (current != getEncryptionKey()) {
      _snackbar.value = context.getString(R.string.error_current_encryption_key_incorrect)
      return
    }

    if (new.isNullOrBlank()) {
      sharedPreferences.edit().putString(ENCRYPTED_PREFS_ENCRYPTION_KEY, null).apply()
      _snackbar.value = context.getString(R.string.message_encryption_key_cleared)
    } else {
      sharedPreferences.edit().putString(ENCRYPTED_PREFS_ENCRYPTION_KEY, new).apply()
      _snackbar.value = context.getString(R.string.message_encryption_key_set)
    }
  }

}