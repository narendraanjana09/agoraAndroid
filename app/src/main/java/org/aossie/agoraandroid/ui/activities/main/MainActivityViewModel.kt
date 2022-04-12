package org.aossie.agoraandroid.ui.activities.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.aossie.agoraandroid.data.Repository.UserRepository
import javax.inject.Inject

class MainActivityViewModel
@Inject
constructor(
  val userRepository: UserRepository
) : ViewModel() {

  private val mutableIsLogout = MutableLiveData<Boolean>()
  private val _getNetworkStatusLiveData = MutableLiveData<Boolean>()
  val getNetworkStatusLiveData = _getNetworkStatusLiveData
  val isLogout: LiveData<Boolean> get() = mutableIsLogout

  fun setLogout(isLogout: Boolean) {
    mutableIsLogout.value = isLogout
  }
  fun onNetworkChanged(isConnected: Boolean) {
    _getNetworkStatusLiveData.value = isConnected
  }

  fun deleteUserData() {
    viewModelScope.launch {
      userRepository.deleteUser()
    }
  }
}