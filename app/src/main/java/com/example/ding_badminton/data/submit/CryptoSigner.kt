package com.example.ding_badminton.data.submit

interface CryptoSigner {
    fun encryptForSubmit(plainJson: String): String
}

class PlainPassCryptoSigner : CryptoSigner {
    override fun encryptForSubmit(plainJson: String): String {
        // M3 占位：当前仅回传明文，后续替换为真实加密流程。
        return plainJson
    }
}

class AesCbcZeroPaddingSigner : CryptoSigner {
    override fun encryptForSubmit(plainJson: String): String {
        return CryptoCodec.encryptZeroPaddedToBase64(plainJson)
    }
}
