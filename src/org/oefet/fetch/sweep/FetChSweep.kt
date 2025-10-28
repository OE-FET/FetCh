package org.oefet.fetch.sweep


import javafx.scene.image.Image
import jisa.experiment.queue.Action
import jisa.experiment.queue.ActionQueue
import jisa.experiment.queue.SweepAction
import jisa.results.Column
import jisa.results.ResultTable
import org.oefet.fetch.FetChEntity
import org.oefet.fetch.FetChEntityAction
import org.oefet.fetch.FetChSweepAction
import org.oefet.fetch.quant.*

abstract class FetChSweep<T>(private val name: String, defaultQuantityName: String, private val quantityType: Type, override val image: Image) : FetChEntity() {

    val queue = ActionQueue()
    val tag   by userInput("Basic", "Name", defaultQuantityName)

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

    open fun generateFinalActions(): List<Action<*>> {
        return emptyList()
    }

    /**
     * Returns the human-readable formatted string representation of given sweep value.
     *
     * @param value The value to turn into a string representation
     */
    abstract fun formatValue(value: T): String

    open fun formatValueForAttribute(value: T): String = formatValue(value)

    fun generate(value: T, actions: List<Action<*>>): List<Action<*>> {

        val generated  = generateForValue(value, actions)
        val quantities = getParameterList() + generateQuantitiesForValue(tag, value)

        generated.forEach {

            if (it is FetChEntityAction) {
                quantities.forEach(it::addParameter)
            }

            if (it is FetChSweepAction<*>) {
                quantities.forEach(it.sweep::addParameter)
                it.regenerateActions()
            }

            it.setAttribute(tag, formatValueForAttribute(value))
            it.addTag("$tag = ${formatValue(value)}")

        }

        return generated

    }

    open fun generateQuantitiesForValue(tag: String, value: T): List<Quantity<*>> {

        if (value is Number) {
            return listOf(DoubleQuantity(tag, quantityType, value.toDouble(), 0.0))
        }

        if (value is XYPoint) {
            return listOf(XYQuantity(tag, tag, quantityType, value.x, value.y, 0.0, 0.0))
        }

        if (value is XYZPoint) {
            return listOf(XYZQuantity(tag, tag, quantityType, value.x, value.y, value.z, 0.0, 0.0, 0.0))
        }

        return listOf(StringQuantity(tag, tag, quantityType, value.toString()))

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

    fun createSweepAction(): FetChSweepAction<T> {
        return FetChSweepAction(name, getValues(), this::generate, this).apply {
            setFormatter(this@FetChSweep::formatValue)
            addFinalActions(generateFinalActions())
        }
    }

}