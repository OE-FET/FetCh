package org.oefet.fetch.logging

import jisa.control.RTask
import jisa.results.Column
import jisa.results.ResultStream
import java.util.*

object Log {

    private var data     : ResultStream?          = null
    private val actions  : MutableList<LogAction> = LinkedList<LogAction>()
    private val COL_TIME : Column<Long>           = Column.ofLongs("Time", "UTC ms")
    private var interval : Long                   = 1000
    private val task     : RTask                  = RTask(interval, this::logStep)

    fun newFile(file: String) {

        data?.close()
        data = ResultStream(file, *(listOf(COL_TIME) + actions.map { it.column }).toTypedArray())

    }

    fun addAction(title: String, yLabel: String, yUnit: String, action: () -> Double) {

        actions += object : LogAction {
            override val title       = title
            override val yLabel      = yLabel
            override val yUnits      = yUnit
            override val column      = Column.ofDoubles(title, yUnit)
            override var isEnabled   = true
            override val value get() = action()
        }

    }

    fun logStep() {

        val time = System.currentTimeMillis()

        data?.mapRow(
            actions.associate { it.column to it.value } + (COL_TIME to time)
        )

    }

    fun close() {
        data?.close()
        data = null
    }

}