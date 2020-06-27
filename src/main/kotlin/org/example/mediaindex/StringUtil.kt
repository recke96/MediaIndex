package org.example.mediaindex

fun String.limit(chars: Int): String {
    return if (this.length <= chars) {
        this
    } else {
        this.substring(0, chars)
    }
}