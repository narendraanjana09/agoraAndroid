package org.aossie.agoraandroid.ui.fragments.electionDetails

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.takusemba.spotlight.Spotlight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.aossie.agoraandroid.R.drawable
import org.aossie.agoraandroid.R.string
import org.aossie.agoraandroid.data.db.PreferenceProvider
import org.aossie.agoraandroid.databinding.FragmentElectionDetailsBinding
import org.aossie.agoraandroid.ui.fragments.BaseFragment
import org.aossie.agoraandroid.utilities.AppConstants
import org.aossie.agoraandroid.utilities.ResponseUI
import org.aossie.agoraandroid.utilities.TargetData
import org.aossie.agoraandroid.utilities.getSpotlight
import org.aossie.agoraandroid.utilities.hide
import org.aossie.agoraandroid.utilities.scrollToView
import org.aossie.agoraandroid.utilities.show
import org.aossie.agoraandroid.utilities.toggleIsEnable
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 */

class ElectionDetailsFragment
@Inject
constructor(
  private val viewModelFactory: ViewModelProvider.Factory,
  private val prefs: PreferenceProvider
) : BaseFragment(viewModelFactory) {

  private var id: String? = null
  private var status: AppConstants.Status? = null
  private val electionDetailsViewModel: ElectionDetailsViewModel by viewModels {
    viewModelFactory
  }

  private lateinit var binding: FragmentElectionDetailsBinding

  private var spotlight: Spotlight? = null
  private var spotlightTargets: ArrayList<TargetData>? = null
  private var currentSpotlightIndex = 0

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = FragmentElectionDetailsBinding.inflate(inflater)
    return binding.root
  }

  override fun onFragmentInitiated() {
    val args =
      ElectionDetailsFragmentArgs.fromBundle(
        requireArguments()
      )
    id = args.id
    electionDetailsViewModel.sessionExpiredListener = this
    setObserver()
    initListeners()

    getElectionById()
    binding.root.doOnLayout {
      checkIsFirstOpen()
    }
  }

  override fun onNetworkConnected() {
    view?.let { getElectionById() }
  }

  private fun setObserver() {
    lifecycleScope.launch {
      electionDetailsViewModel.getDeleteElectionStateFlow.collect {
        if (it != null) {
          when (it.status) {
            ResponseUI.Status.LOADING -> {
              binding.progressBar.show()
              binding.buttonDelete.toggleIsEnable()
            }
            ResponseUI.Status.SUCCESS -> {
              lifecycleScope.launch {
                prefs.setUpdateNeeded(true)
              }
              Navigation.findNavController(binding.root)
                .navigate(
                  ElectionDetailsFragmentDirections.actionElectionDetailsFragmentToHomeFragment()
                )
            }
            ResponseUI.Status.ERROR -> {
              notify(it.message)
              binding.progressBar.hide()
              binding.buttonDelete.toggleIsEnable()
            }
            else -> {}
          }
        }
      }
    }
  }

  private fun initListeners() {
    binding.buttonBallot.setOnClickListener {
      val action =
        ElectionDetailsFragmentDirections.actionElectionDetailsFragmentToBallotFragment(
          id!!
        )
      Navigation.findNavController(binding.root)
        .navigate(action)
    }
    binding.buttonVoters.setOnClickListener {
      val action =
        ElectionDetailsFragmentDirections.actionElectionDetailsFragmentToVotersFragment(
          id!!
        )
      Navigation.findNavController(binding.root)
        .navigate(action)
    }
    binding.buttonInviteVoters.setOnClickListener {
      if (status == AppConstants.Status.FINISHED) {
        notify(resources.getString(string.election_finished))
      } else {
        val action =
          ElectionDetailsFragmentDirections.actionElectionDetailsFragmentToInviteVotersFragment(
            id!!
          )
        Navigation.findNavController(binding.root)
          .navigate(action)
      }
    }
    binding.buttonResult.setOnClickListener {
      if (status == AppConstants.Status.PENDING) {
        notify(resources.getString(string.election_not_started))
      } else {
        if (isConnected) {
          val action =
            ElectionDetailsFragmentDirections.actionElectionDetailsFragmentToResultFragment(
              id!!
            )
          Navigation.findNavController(binding.root)
            .navigate(action)
        } else {
          notify(resources.getString(string.no_network))
        }
      }
    }
    binding.buttonDelete.setOnClickListener {
      when (status) {
        AppConstants.Status.ACTIVE -> notify(
          resources.getString(string.active_elections_not_started)
        )
        AppConstants.Status.FINISHED -> electionDetailsViewModel.deleteElection(id)
        AppConstants.Status.PENDING -> electionDetailsViewModel.deleteElection(id)
        else -> {}
      }
    }
  }

  private fun getElectionById() {
    lifecycleScope.launch {
      electionDetailsViewModel.getElectionById(id ?: "").collect {
        if (it != null) {
          Timber.d(it.toString())
          try {
            binding.tvName.text = it.name
            binding.tvDescription.text = it.description
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
            val formattedStartingDate: Date = formatter.parse(it.start!!) as Date
            val formattedEndingDate: Date = formatter.parse(it.end!!) as Date
            val currentDate = Calendar.getInstance()
              .time
            val outFormat = SimpleDateFormat("dd-MM-yyyy 'at' HH:mm:ss", Locale.ENGLISH)
            // set end and start date
            binding.tvEndDate.text = outFormat.format(formattedEndingDate)
            binding.tvStartDate.text = outFormat.format(formattedStartingDate)
            // set label color and election status
            binding.label.text =
              getEventStatus(currentDate, formattedStartingDate, formattedEndingDate)?.name
            binding.label.setBackgroundResource(
              getEventColor(
                currentDate,
                formattedStartingDate,
                formattedEndingDate
              )
            )
            status = getEventStatus(currentDate, formattedStartingDate, formattedEndingDate)
          } catch (e: ParseException) {
            e.printStackTrace()
          }
          // add candidates name
          val mCandidatesName = StringBuilder()
          val candidates = it.candidates
          if (candidates != null) {
            for (j in 0 until candidates.size) {
              mCandidatesName.append(candidates[j])
              if (j != candidates.size - 1) {
                mCandidatesName.append(", ")
              }
            }
          }
          binding.tvCandidateList.text = mCandidatesName
        }
      }
    }
  }

  private fun getEventStatus(
    currentDate: Date,
    formattedStartingDate: Date?,
    formattedEndingDate: Date?
  ): AppConstants.Status? {
    return when {
      currentDate.before(formattedStartingDate) -> AppConstants.Status.PENDING
      currentDate.after(formattedStartingDate) && currentDate.before(
        formattedEndingDate
      ) -> AppConstants.Status.ACTIVE
      currentDate.after(formattedEndingDate) -> AppConstants.Status.FINISHED
      else -> null
    }
  }

  private fun getEventColor(
    currentDate: Date,
    formattedStartingDate: Date?,
    formattedEndingDate: Date?
  ): Int {
    return when {
      currentDate.before(formattedStartingDate) -> drawable.pending_election_label
      currentDate.after(formattedStartingDate) && currentDate.before(
        formattedEndingDate
      ) -> drawable.active_election_label
      currentDate.after(formattedEndingDate) -> drawable.finished_election_label
      else -> drawable.finished_election_label
    }
  }

  private fun checkIsFirstOpen() {
    lifecycleScope.launch {
      if (!prefs.isDisplayed(binding.root.id.toString())
        .first()
      ) {
        spotlightTargets = getSpotlightTargets()
        prefs.setDisplayed(binding.root.id.toString())
        showSpotlight()
      }
    }
  }

  private fun showSpotlight() {
    spotlightTargets?.let {
      if (currentSpotlightIndex in it.indices) {
        scrollToView(binding.scrollView, it[currentSpotlightIndex].targetView)
        spotlight = requireActivity().getSpotlight(
          it[currentSpotlightIndex++],
          {
            destroySpotlight()
          },
          {
            it.clear()
            destroySpotlight()
          },
          {
            if (isAdded) {
              showSpotlight()
            }
          }
        )
        spotlight?.start()
      }
    }
  }

  private fun getSpotlightTargets(): ArrayList<TargetData> {
    val targetData = ArrayList<TargetData>()
    targetData.add(
      TargetData(
        binding.label, getString(string.election_status),
        getString(string.status_spotlight)
      )
    )
    targetData.add(
      TargetData(
        binding.buttonDelete, getString(string.delete),
        getString(string.delete_spotlight)
      )
    )
    targetData.add(
      TargetData(
        binding.buttonInviteVoters, getString(string.invite_voter),
        getString(string.invite_spotlight)
      )
    )
    targetData.add(
      TargetData(
        binding.buttonVoters, getString(string.voters),
        getString(string.voters_spotlight)
      )
    )
    targetData.add(
      TargetData(
        binding.buttonBallot, getString(string.ballot),
        getString(string.ballot_spotlight)
      )
    )
    targetData.add(
      TargetData(
        binding.buttonResult, getString(string.result),
        getString(string.result_spotlight)
      )
    )
    return targetData
  }

  private fun destroySpotlight() {
    spotlight?.finish()
    spotlight = null
  }

  override fun onPause() {
    super.onPause()
    destroySpotlight()
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    destroySpotlight()
  }
}
