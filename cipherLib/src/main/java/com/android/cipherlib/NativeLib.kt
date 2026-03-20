package com.android.cipherlib

import androidx.annotation.Keep

@Keep
object NativeLib {
    external fun getCypherKey(): String
    external fun getClaudeExpertKey(): String
    fun loadLibrary() {
        try {
            System.loadLibrary("cypher")
        } catch (_: Exception) {
        } catch (_: UnsatisfiedLinkError) {
        }
    }
}


// usage: shiftRepository.getShift(NativeLib.getClaudeExpertKey())
