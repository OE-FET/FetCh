package org.oefet.fetch.sweep


import jisa.experiment.queue.Action
import jisa.experiment.queue.ActionQueue
import jisa.experiment.queue.SweepAction
import jisa.results.Column
import jisa.results.ResultTable
import org.oefet.fetch.FetChEntity

abstract class FetChSweep<T>(private val name: String, private val tag: String) : FetChEntity() {

    val queue = ActionQueue()

    /**
     * Returns the list of all values this sweep is to sweep over.
     *
     * @return List of sweep values
     */
    abstract fun getValues(): List<T>

    /**
     * Returns the set of actions to undertake for the given sweep value - this should include any automatically
     * generated actions for the purposes of sweeping (i.e. changing temperature etc).
     *
     * @param value The sweep value
     * @param actions The list of actions to perform for this sweep value
     *
     * @return The complete list of actions for this sweep value (including automatically added actions)
     */
    abstract fun generateForValue(value: T, actions: List<Action<*>>): List<Action<*>>

    /**
     * Returns the human-readable formatted string representation of given sweep value.
     *
     * @param value The value to turn into a string representation
     */
    abstract fun formatValue(value: T): String

    open fun formatValueForAttribute(value: T): String = formatValue(value)

    fun generate(value: T, actions: List<Action<*>>): List<Action<*>> {

        val generated = generateForValue(value, actions)

        generated.forEach {

            it.setAttribute(tag, formatValueForAttribute(value))
            it.addTag("$tag = ${formatValue(value)}")

        }

        return generated

    }

    override fun getName(): String {
        return this.name
    }

    override fun getColumns(): Array<Column<*>> {
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

    fun createSweepAction(): SweepAction<T> {
        return SweepAction(name, getValues(), this::generate).apply { setFormatter(this@FetChSweep::formatValue) }
    }

}