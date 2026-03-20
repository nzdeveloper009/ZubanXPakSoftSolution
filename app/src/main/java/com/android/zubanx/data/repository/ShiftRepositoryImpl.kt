package com.android.zubanx.data.repository

import com.android.zubanx.domain.repository.ShiftRepository

class ShiftRepositoryImpl : ShiftRepository {
    override fun getShift(input: String): String {
        val actualShift = 26 - (9 % 26)
        val output = StringBuilder(input.length)

        for (c in input) {
            when (c) {
                in 'A'..'Z' -> {
                    val shiftedChar = 'A' + (c - 'A' + actualShift) % 26
                    output.append(shiftedChar)
                }

                in 'a'..'z' -> {
                    val shiftedChar = 'a' + (c - 'a' + actualShift) % 26
                    output.append(shiftedChar)
                }

                else -> output.append(c)
            }
        }
        return output.toString()
    }


}