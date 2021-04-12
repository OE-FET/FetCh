package org.oefet.fetch.sweep

import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.experiment.queue.Action
import jisa.experiment.queue.ActionQueue
import jisa.experiment.queue.SweepAction
import org.oefet.fetch.FetChEntity

abstract class FetChSweep<T>(private val name: String, private val tag: String) : FetChEntity() {

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

    fun generate(value: T, actions: List<Action<*>>): List<Action<*>> {

        val generated = generateForValue(value, actions)

        generated.forEach {
            it.setAttribute(tag, formatValue(value))
            it.addTag("$tag = ${formatValue(value)}")
        }

        return generated

    }

    abstract fun formatValue(value: T) : String

    fun createSweepAction(): SweepAction<T> {
        return SweepAction(name, getValues(), this::generate).apply { setFormatter(this@FetChSweep::formatValue) }
    }

}