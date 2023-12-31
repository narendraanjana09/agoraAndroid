package org.aossie.agoraandroid.domain.useCases.profile

import org.aossie.agoraandroid.domain.repository.UserRepository
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(
  private val repository: UserRepository
) {
  suspend operator fun invoke(
    password: String
  ): List<String> {
    return repository.changePassword(password)
  }
}
