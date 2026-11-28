package com.example.ztemeshmanager

object ZteSession {
    @Volatile var client: ZteApiClient? = null
    @Volatile var routerUrl: String = RouterDefaults.urlFor(RouterDefaults.HV3601P)
    @Volatile var authenticated: Boolean = false
}
