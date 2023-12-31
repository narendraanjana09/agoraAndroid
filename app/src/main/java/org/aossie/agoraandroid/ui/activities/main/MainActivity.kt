package org.aossie.agoraandroid.ui.activities.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager.LayoutParams
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions.Builder
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.facebook.login.LoginManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aossie.agoraandroid.AgoraApp
import org.aossie.agoraandroid.R
import org.aossie.agoraandroid.R.string
import org.aossie.agoraandroid.data.db.PreferenceProvider
import org.aossie.agoraandroid.databinding.ActivityMainBinding
import org.aossie.agoraandroid.ui.fragments.elections.CalendarViewElectionFragment
import org.aossie.agoraandroid.ui.fragments.home.HomeFragment
import org.aossie.agoraandroid.ui.fragments.settings.SettingsFragment
import org.aossie.agoraandroid.ui.fragments.welcome.WelcomeFragment
import org.aossie.agoraandroid.utilities.AppConstants
import org.aossie.agoraandroid.utilities.animGone
import org.aossie.agoraandroid.utilities.animVisible
import org.aossie.agoraandroid.utilities.canAuthenticateBiometric
import org.aossie.agoraandroid.utilities.notifyNetworkChanged
import org.aossie.agoraandroid.utilities.snackbar
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityMainBinding

  private lateinit var navController: NavController
  private var isInitiallyNetworkConnected: Boolean = true

  @Inject
  lateinit var viewModelFactory: ViewModelProvider.Factory

  @Inject
  lateinit var mainFragmentFactory: FragmentFactory

  @Inject
  lateinit var prefs: PreferenceProvider

  private val viewModel: MainActivityViewModel by viewModels {
    viewModelFactory
  }

  override fun onCreate(savedInstanceState: Bundle?) {

    (application as AgoraApp).appComponent.inject(this)

    supportFragmentManager.fragmentFactory = mainFragmentFactory

    setTheme(R.style.AppTheme)
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayShowTitleEnabled(false)

    intent?.getStringExtra(AppConstants.SHOW_SNACKBAR_KEY)
      ?.let {
        binding.root.snackbar(it)
      }
    val hostFragment = supportFragmentManager.findFragmentById(R.id.host_fragment)
    if (hostFragment is NavHostFragment)
      navController = hostFragment.navController

    navController.addOnDestinationChangedListener { _, destination, _ ->
      setToolbar(destination)
      handleBottomNavVisibility(destination.id)
      handleStatusBar(destination.id)
    }

    NavigationUI.setupWithNavController(binding.bottomNavigation, navController)
    GlobalScope.launch {
      prefs.setUpdateNeeded(true)
      if (prefs.getIsLoggedIn()
        .first()
      ) {
        if (prefs.isBiometricEnabled().first() && canAuthenticateBiometric())
          withContext(Dispatchers.Main) { provideOfBiometricPrompt().authenticate(getPromtInfo()) }
        else
          withContext(Dispatchers.Main) { navController.navigate(R.id.homeFragment) }
      }
    }

    initObservers()
  }

  private fun getPromtInfo() =
    PromptInfo.Builder()
      .setTitle(getString(string.bio_auth))
      .setDescription(getString(string.bio_auth_msg))
      .apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
          setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        else {
          setDeviceCredentialAllowed(true)
          setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        }
      }.build()

  private fun provideOfBiometricPrompt(): BiometricPrompt {
    val executor = ContextCompat.getMainExecutor(this)

    val callback = object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        binding.root.snackbar(getString(string.something_went_wrong))
      }

      override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        binding.root.snackbar(getString(string.auth_failed))
      }

      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        navController.navigate(R.id.homeFragment)
      }
    }

    return BiometricPrompt(this, executor, callback)
  }

  private fun setToolbar(destination: NavDestination) {
    handleBackButton(destination.id)
    binding.tvTitle.text = destination.label
  }

  private fun handleBottomNavVisibility(id: Int) {
    when (id) {
      R.id.calendarViewElectionFragment,
      R.id.homeFragment,
      R.id.electionsFragment,
      R.id.settingsFragment
      -> binding.bottomNavigation.animVisible()
      else -> binding.bottomNavigation.animGone()
    }
  }

  private fun handleStatusBar(id: Int) {
    when (id) {
      R.id.welcomeFragment,
      R.id.loginFragment,
      R.id.signUpFragment,
      R.id.forgotPasswordFragment
      -> {
        window.addFlags(LayoutParams.FLAG_TRANSLUCENT_STATUS)
        supportActionBar?.hide()
      }
      R.id.settingsFragment
      -> {
        supportActionBar?.hide()
      }
      else -> {
        window.clearFlags(LayoutParams.FLAG_TRANSLUCENT_STATUS)
        supportActionBar?.show()
      }
    }
  }

  override fun onBackPressed() {
    val hostFragment = supportFragmentManager.findFragmentById(R.id.host_fragment)
    if (hostFragment is NavHostFragment) {
      when (hostFragment.childFragmentManager.fragments.first()) {
        is HomeFragment,
        is WelcomeFragment -> finish()
        is SettingsFragment,
        is CalendarViewElectionFragment -> navController.navigate(R.id.homeFragment)
        else -> super.onBackPressed()
      }
    } else {
      super.onBackPressed()
    }
  }

  private fun handleBackButton(id: Int) {
    when (id) {
      R.id.aboutFragment,
      R.id.shareWithOthersFragment,
      R.id.contactUsFragment,
      R.id.profileFragment -> binding.ivBack.let {
        it.visibility = View.VISIBLE
        it.setOnClickListener { onBackPressed() }
      }
      else -> binding.ivBack.visibility = View.GONE
    }
  }

  private fun initObservers() {
    lifecycleScope.launch {
      viewModel.getNetworkStatusStateFlow.collect { isConnected ->
        if (isConnected == true) {
          if (!isInitiallyNetworkConnected)
            binding.root.notifyNetworkChanged(true, binding.bottomNavigation)
          isInitiallyNetworkConnected = true
        } else {
          isInitiallyNetworkConnected = false
          binding.root.notifyNetworkChanged(false, binding.bottomNavigation)
        }
      }
    }
    lifecycleScope.launch {
      viewModel.isLogout.collect {
        if (it == true) logout()
      }
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    val navHostFragment = supportFragmentManager.findFragmentById(R.id.host_fragment)
    val childFragments = navHostFragment?.childFragmentManager?.fragments
    childFragments?.forEach { it.onActivityResult(requestCode, resultCode, data) }
  }

  private fun logout() {
    lifecycleScope.launch {
      if (prefs.getIsLoggedIn()
        .first()
      ) binding.root.snackbar(resources.getString(R.string.token_expired))
      if (prefs.getIsFacebookUser().first()) {
        LoginManager.getInstance()
          .logOut()
      }
    }
    viewModel.deleteUserData()
    val navBuilder = Builder()
    navBuilder.setEnterAnim(R.anim.slide_in_left)
      .setExitAnim(R.anim.slide_out_right)
      .setPopEnterAnim(R.anim.slide_in_right)
      .setPopExitAnim(R.anim.slide_out_left)
    navController.navigate(R.id.welcomeFragment, null, navBuilder.build())
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    intent?.getStringExtra(AppConstants.SHOW_SNACKBAR_KEY)
      ?.let {
        binding.root.snackbar(it)
      }
  }
}
