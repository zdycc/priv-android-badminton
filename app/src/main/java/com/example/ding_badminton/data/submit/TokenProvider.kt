package com.example.ding_badminton.data.submit

import com.example.ding_badminton.data.local.UserPrefsRepository
import kotlinx.coroutines.flow.first

interface TokenProvider {
    suspend fun getUserToken(): String?
}

class LocalTokenProvider(
    private val userPrefsRepository: UserPrefsRepository,
) : TokenProvider {
    override suspend fun getUserToken(): String? {
        return userPrefsRepository.userTokenFlow.first()?.trim()?.takeIf { it.isNotEmpty() }
    }
}
