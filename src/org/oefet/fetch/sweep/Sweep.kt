package org.oefet.fetch.sweep

import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.experiment.queue.Action
import jisa.experiment.queue.ActionQueue
import jisa.experiment.queue.SweepAction
import org.oefet.fetch.FEntity

abstract class Sweep<T>(private val name: String) : FEntity() {

    val queue = ActionQueue()

    override fun getName(): String {
        return this.name
    }

    override fun getColumns(): Array<Col> {
        return emptyArray()
    }

    override fun run(results: ResultTable) {

    }

    override fun onFinish() {

    }

    override fun setLabel(value: String) {
    }

    override fun onError() {

    }

    override fun onInterrupt() {

    }

    abstract fun getValues(): List<T>

    abstract fun generateForValue(value: T, actions: List<Action<*>>): List<Action<*>>

    abstract fun formatValue(value: T) : String

    fun createSweepAction(): SweepAction<T> {
        return SweepAction(name, getValues(), this::generateForValue).apply { setFormatter(this@Sweep::formatValue) }
    }

}