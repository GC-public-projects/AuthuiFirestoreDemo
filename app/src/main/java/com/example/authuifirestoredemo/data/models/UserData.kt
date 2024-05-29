package com.example.authuifirestoredemo.data.models

data class UserData (
    val nickname:String?,
    val age: Int?
) {
    constructor() : this(null, null)
}
