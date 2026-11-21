package com.example.ztemeshmanager

object ZteSession {
    @Volatile var client: ZteApiClient? = null
    @Volatile var routerUrl: String = "https://192.168.1.11/"
    @Volatile var authenticated: Boolean = false
}
