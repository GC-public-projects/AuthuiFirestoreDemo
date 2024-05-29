package com.example.authuifirestoredemo.data.models

data class City (
    val name:String?,
    val state:String?,
    val country: String?,
    val timestamp: Long? // to retrieve docs by order (auto id not following numbers)
) {
    constructor() : this(null, null, null, null)
}
