package org.oefet.fetch.logging

import jisa.results.Column
import jisa.results.ResultStream
import java.util.*

class Dash(val name: String) {

    var data: ResultStream? = null

    val values     = LinkedList<DashValue>()
    val plots      = LinkedList<DashPlot>()
    val timeColumn = Column.ofLongs("Time", "UTC ms")
    var isEnabled  = true;

    init {
        Log.loggers += this
    }

    fun newFile(file: String) {

        data?.close()
        data = ResultStream(file, *(listOf(timeColumn) + values.map { it.column }).toTypedArray())

    }

    fun logStep(time: Long) {

        data?.mapRow(

            values.associate {

                if (it.isEnabled) {

                    try {
                        it.column to it.value
                    } catch (_: Throwable) {
                        it.column to Double.NaN
                    }

                } else {
                    it.column to Double.NaN
                }

            } + (timeColumn to time)

        )

    }

    fun close() {
        data?.close()
        data = null
    }

    fun autoGenerateActions() {
        values += Log.getSources().map { DashValue(it.name, it.units, listOf(it), "a") }
    }

    fun clearActions() {
        values.clear()
    }

    fun addAction(action: DashValue) {
        values.add(action)
    }

    fun removeAction(action: DashValue) {
        values.remove(action)
    }

    fun delete() {
        Log.loggers -= this
    }

}