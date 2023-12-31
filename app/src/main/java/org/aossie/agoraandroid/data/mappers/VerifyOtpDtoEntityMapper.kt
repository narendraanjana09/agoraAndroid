package org.aossie.agoraandroid.data.mappers

import org.aossie.agoraandroid.data.network.dto.VerifyOtpDto
import org.aossie.agoraandroid.domain.model.VerifyOtpDtoModel
import javax.inject.Inject

class VerifyOtpDtoEntityMapper @Inject constructor() {
  fun mapToEntity(domainModel: VerifyOtpDtoModel): VerifyOtpDto {
    return VerifyOtpDto(
      crypto = domainModel.crypto,
      otp = domainModel.otp,
      isTrusted = domainModel.isTrusted
    )
  }
}
