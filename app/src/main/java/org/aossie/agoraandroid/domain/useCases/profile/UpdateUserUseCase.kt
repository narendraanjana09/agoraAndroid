package org.aossie.agoraandroid.domain.useCases.profile

import org.aossie.agoraandroid.data.mappers.Mappers
import org.aossie.agoraandroid.data.mappers.UpdateUserDtoEntityMapper
import org.aossie.agoraandroid.domain.model.UpdateUserDtoModel
import org.aossie.agoraandroid.domain.repository.UserRepository
import javax.inject.Inject

class UpdateUserUseCase @Inject constructor(
  private val repository: UserRepository,
  private val mappers: Mappers
) {
  private val updateUserDtoEntityMapper = UpdateUserDtoEntityMapper()
  suspend operator fun invoke(
    updateUserDtoModel: UpdateUserDtoModel
  ): List<String> {
    return repository.updateUser(updateUserDtoEntityMapper.mapToEntity(updateUserDtoModel))
  }
}
