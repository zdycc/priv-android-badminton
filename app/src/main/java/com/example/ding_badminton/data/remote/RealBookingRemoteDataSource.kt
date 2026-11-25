package com.example.ding_badminton.data.remote

import com.example.ding_badminton.data.debug.RequestLogStore
import com.example.ding_badminton.data.submit.CryptoSigner
import com.example.ding_badminton.data.submit.PayloadBuilder
import com.example.ding_badminton.data.submit.SubmitApi
import com.example.ding_badminton.data.submit.TokenProvider
import com.example.ding_badminton.data.submit.model.SubmitRequest
import com.example.ding_badminton.data.submit.model.SubmitPayload
import com.example.ding_badminton.domain.model.BookingConfig
import com.example.ding_badminton.domain.model.BookingOutcome
import com.example.ding_badminton.domain.usecase.FailureClassifier

class RealBookingRemoteDataSource(
    private val tokenProvider: TokenProvider,
    private val payloadBuilder: PayloadBuilder,
    private val cryptoSigner: CryptoSigner,
    private val submitApi: SubmitApi,
    private val failureClassifier: FailureClassifier,
) : BookingRemoteDataSource {
    override suspend fun submitOrder(config: BookingConfig): BookingOutcome {
        val token = tokenProvider.getUserToken()
        if (token.isNullOrBlank()) {
            return BookingOutcome(
                success = false,
                reason = "登录态缺失，请先完成登录",
                retryable = false,
            )
        }
        val payload = payloadBuilder.build(config)
        val payloadJson = payload.toSubmitJson()
        RequestLogStore.append("payload", payloadJson.take(220))
        val encrypted = cryptoSigner.encryptForSubmit(payloadJson)
        val result =
            submitApi.submitVenueOrder(
                SubmitRequest(
                    encryptedParams = encrypted,
                    userToken = token,
                ),
            )
        return if (result.ok) {
            BookingOutcome(
                success = true,
                reason = result.message.ifBlank { "预约成功" },
                retryable = false,
            )
        } else {
            failureClassifier.classify(result.message.ifBlank { "提交失败（真实接口）" })
        }
    }

    private fun SubmitPayload.toSubmitJson(): String {
        val timeArrJson =
            timeArr.joinToString(
                separator = ",",
                prefix = "[",
                postfix = "]",
            ) { """{"siteId":"${it.siteId}","timeId":"${it.timeId}"}""" }
        val peopleJson =
            people.joinToString(
                separator = ",",
                prefix = "[",
                postfix = "]",
            ) { """{"name":"${it.name}","phone":"${it.phone}"}""" }
        return """{"id":"$id","day":"$day","mark":"$mark","timeArr":$timeArrJson,"people":$peopleJson}"""
    }
}
