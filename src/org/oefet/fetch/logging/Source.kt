package org.oefet.fetch.logging

class Source(val name: String, val units: String, val expression: () -> Double) {

    var cached: Double = 0.0
    var isUsed: Boolean = false

    fun update() {
        cached = expression()
    }

    fun checkUse() {
        isUsed = Log.loggers.filter{ it.isEnabled }.flatMap { a -> a.values.filter{ it.isEnabled }.flatMap { b -> b.sources } }.any { it == this }
    }

}