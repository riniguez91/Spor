package com.example.spor_tfg

import android.app.Application


class MyApp : Application() {
    var doc: HashMap<Any, Any>? = null

    fun getDocVar(): HashMap<Any, Any> {
        return doc!!
    }

    fun setDocVar(value: HashMap<Any, Any>) {
        doc = value
    }
}