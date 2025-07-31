package com.github.d0lfin.sona.services

import com.intellij.openapi.components.Service

@Service
class MyProjectService() {
    fun getRandomNumber() = (1..100).random()
}
