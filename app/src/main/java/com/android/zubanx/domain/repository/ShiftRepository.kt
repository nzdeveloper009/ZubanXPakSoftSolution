package com.android.zubanx.domain.repository

interface ShiftRepository {
    fun getShift(input: String): String
}