package org.aossie.agoraandroid.ui.fragments.electionDetails

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aossie.agoraandroid.R.string
import org.aossie.agoraandroid.domain.model.BallotDtoModel
import org.aossie.agoraandroid.domain.model.ElectionModel
import org.aossie.agoraandroid.domain.model.VotersDtoModel
import org.aossie.agoraandroid.domain.model.WinnerDtoModel
import org.aossie.agoraandroid.domain.useCases.electionDetails.ElectionDetailsUseCases
import org.aossie.agoraandroid.ui.fragments.auth.SessionExpiredListener
import org.aossie.agoraandroid.utilities.ApiException
import org.aossie.agoraandroid.utilities.FileUtils
import org.aossie.agoraandroid.utilities.NoInternetException
import org.aossie.agoraandroid.utilities.ResponseUI
import org.aossie.agoraandroid.utilities.SessionExpirationException
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

class ElectionDetailsViewModel
@Inject
constructor(
  private val electionDetailsUseCases: ElectionDetailsUseCases
) : ViewModel() {

  private val _getVoterResponseStateFlow = MutableStateFlow<ResponseUI<VotersDtoModel>?>(null)
  var getVoterResponseStateFlow: StateFlow<ResponseUI<VotersDtoModel>?> = _getVoterResponseStateFlow
  private val _getBallotResponseStateFlow = MutableStateFlow<ResponseUI<BallotDtoModel>?>(null)
  var getBallotResponseStateFlow: StateFlow<ResponseUI<BallotDtoModel>?> =
    _getBallotResponseStateFlow
  private val _getShareResponseStateFlow = MutableStateFlow<ResponseUI<Uri>?>(null)
  var getShareResponseStateFlow: StateFlow<ResponseUI<Uri>?> = _getShareResponseStateFlow
  private val _getResultResponseStateFlow = MutableStateFlow<ResponseUI<WinnerDtoModel>?>(null)
  var getResultResponseStateFlow: StateFlow<ResponseUI<WinnerDtoModel>?> =
    _getResultResponseStateFlow
  private val _getDeleteElectionStateFlow = MutableStateFlow<ResponseUI<WinnerDtoModel>?>(null)
  var getDeleteElectionStateFlow: StateFlow<ResponseUI<WinnerDtoModel>?> =
    _getDeleteElectionStateFlow

  lateinit var sessionExpiredListener: SessionExpiredListener

  fun getElectionById(id: String): Flow<ElectionModel> {
    return electionDetailsUseCases.getElectionById(id)
  }

  fun getBallot(
    id: String?
  ) {
    _getBallotResponseStateFlow.value = ResponseUI.loading()
    viewModelScope.launch {
      try {
        val response: List<BallotDtoModel> = electionDetailsUseCases.getBallots(id)
        Timber.d(response.toString())
        _getBallotResponseStateFlow.value = ResponseUI.success(response)
      } catch (e: ApiException) {
        _getBallotResponseStateFlow.value = ResponseUI.error(e.message)
      } catch (e: SessionExpirationException) {
        sessionExpiredListener.onSessionExpired()
      } catch (e: NoInternetException) {
        _getBallotResponseStateFlow.value = ResponseUI.error(e.message)
      } catch (e: Exception) {
        _getBallotResponseStateFlow.value = ResponseUI.error(e.message)
      }
    }
  }

  fun getVoter(
    id: String?
  ) {
    _getVoterResponseStateFlow.value = ResponseUI.loading()
    viewModelScope.launch {
      try {
        val response = electionDetailsUseCases.getVoters(id)
        Timber.d(response.toString())
        _getVoterResponseStateFlow.value = ResponseUI.success(response)
      } catch (e: ApiException) {
        _getVoterResponseStateFlow.value = ResponseUI.error(e.message)
      } catch (e: SessionExpirationException) {
        sessionExpiredListener.onSessionExpired()
      } catch (e: NoInternetException) {
        _getVoterResponseStateFlow.value = ResponseUI.error(e.message)
      } catch (e: Exception) {
        _getVoterResponseStateFlow.value = ResponseUI.error(e.message)
      }
    }
  }

  fun deleteElection(
    id: String?
  ) {
    _getDeleteElectionStateFlow.value = ResponseUI.loading()
    viewModelScope.launch {
      try {
        val response = electionDetailsUseCases.deleteElection(id)
        Timber.d(response.toString())
        _getDeleteElectionStateFlow.value = ResponseUI.success(response[1])
      } catch (e: ApiException) {
        _getDeleteElectionStateFlow.value = ResponseUI.error(e.message)
      } catch (e: SessionExpirationException) {
        sessionExpiredListener.onSessionExpired()
      } catch (e: NoInternetException) {
        _getDeleteElectionStateFlow.value = ResponseUI.error(e.message)
      } catch (e: Exception) {
        _getDeleteElectionStateFlow.value = ResponseUI.error(e.message)
      }
    }
  }

  fun getResult(
    id: String?
  ) {
    _getResultResponseStateFlow.value = ResponseUI.loading()
    viewModelScope.launch {
      try {
        val response = electionDetailsUseCases.getResult(id)
        if (!response.isNullOrEmpty())
          _getResultResponseStateFlow.value = ResponseUI.success(response[0])
        else
          _getResultResponseStateFlow.value = ResponseUI.success()
      } catch (e: ApiException) {
        _getResultResponseStateFlow.value = ResponseUI.error(e.message)
      } catch (e: SessionExpirationException) {
        sessionExpiredListener.onSessionExpired()
      } catch (e: NoInternetException) {
        _getResultResponseStateFlow.value = ResponseUI.error(e.message)
      } catch (e: Exception) {
        _getResultResponseStateFlow.value = ResponseUI.error(e.message)
      }
    }
  }

  fun createImage(
    context: Context,
    bitmap: Bitmap
  ) {
    viewModelScope.launch {
      val btm = withContext(Dispatchers.IO) {
        FileUtils.saveBitmap(context, bitmap)
      }
      btm?.let {
        _getShareResponseStateFlow.value = ResponseUI.success(it)
      } ?: run {
        _getShareResponseStateFlow.value =
          ResponseUI.error(context.getString(string.something_went_wrong_please_try_again_later))
      }
    }
  }

  fun createExcelFile(
    context: Context,
    winnerDto: WinnerDtoModel,
    id: String
  ) {
    viewModelScope.launch {
      val workbook = withContext(Dispatchers.Default) {
        createWorkbook(context, winnerDto)
      }
      try {
        val uri = withContext(Dispatchers.IO) {
          writeToExcelFile(context, workbook, id)
        }
        _getShareResponseStateFlow.value = ResponseUI.success(uri)
      } catch (e: FileNotFoundException) {
        _getShareResponseStateFlow.value =
          ResponseUI.error(context.getString(string.file_not_available))
      } catch (e: IOException) {
        _getShareResponseStateFlow.value =
          ResponseUI.error(context.getString(string.cannot_write_file))
      } catch (e: Exception) {
        _getShareResponseStateFlow.value =
          ResponseUI.error(context.getString(string.something_went_wrong_please_try_again_later))
      }
    }
  }

  private fun createWorkbook(
    context: Context,
    winnerDto: WinnerDtoModel
  ): XSSFWorkbook {
    val workbook = XSSFWorkbook()
    val sheet: XSSFSheet = workbook.createSheet(context.getString(string.result))
    val title = sheet.createRow(0)
    title.createCell(0)
      .setCellValue(context.getString(string.candidate))
    title.createCell(1)
      .setCellValue(context.getString(string.votes))
    val winner = sheet.createRow(1)
    winner.createCell(0)
      .setCellValue(winnerDto.candidate?.name)
    winner.createCell(1)
      .setCellValue(winnerDto.score?.numerator.toString())
    val others = sheet.createRow(2)
    others.createCell(0)
      .setCellValue(context.getString(string.others))
    others.createCell(1)
      .setCellValue(winnerDto.score?.denominator.toString())
    return workbook
  }

  private fun writeToExcelFile(
    context: Context,
    workbook: XSSFWorkbook,
    id: String
  ): Uri {
    val folderName = context.getExternalFilesDir(null)?.absolutePath
    val folder = File("$folderName", context.getString(string.result))
    if (!folder.exists()) {
      folder.mkdirs()
    }
    val fileName = "$id.xlsx"
    val file = File(folder, fileName)
    if (file.exists()) file.delete()
    try {
      val fileOut = FileOutputStream(file)
      workbook.write(fileOut)
      fileOut.flush()
      fileOut.close()
      return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
      )
    } catch (e: Exception) {
      throw e
    }
  }
}
