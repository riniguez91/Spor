package com.example.spor_tfg

import android.app.Application


class MyApp : Application() {
    var doc: HashMap<Any, Any> = HashMap()
    var doc_team: HashMap<Any, Any> = HashMap()

    fun getDocVar(): HashMap<Any, Any> {
        return doc
    }

    fun setDocVar(value: HashMap<Any, Any>) {
        doc = value
    }

    fun getDocTeam(): HashMap<Any, Any> {
        return doc_team
    }

    fun setDocTeam(value: HashMap<Any, Any>) {
        doc_team = value
    }
}