package com.galian.samples

import android.text.TextUtils

class Utils {
    companion object {
        fun addBlueColor(str: String, word: String): String? {
            return if (TextUtils.isEmpty(word)) {
                str
            } else str.replace("(?i)($word)".toRegex(), "<font color=#0000ff>$1</font>")
        }

        fun addRedColor(str: String, word: String): String? {
            return if (TextUtils.isEmpty(word)) {
                str
            } else str.replace("(?i)($word)".toRegex(), "<font color=#ff0000>$1</font>")
        }
    }
}