package org.oefet.fetch.measurement

import jisa.devices.interfaces.*
import jisa.devices.Configuration
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.gui.Plot
import jisa.maths.Range
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.ResultFile
import java.util.*
import kotlin.reflect.KClass

abstract class FMeasurement(private val name: String, label: String, val type: String) : Measurement() {

    private val labelProperty = StringParameter("Basic", "Name", null, label)
    private val setters       = LinkedList<() -> Unit>()
    private val errors        = LinkedList<String>()

    protected var gdSMU:    SMU?      = null
    protected var sdSMU:    SMU?      = null
    protected var sgSMU:    SMU?      = null
    protected var lockIn:   DPLockIn? = null
    protected var preAmp:   VPreAmp?  = null
    protected var dcPower:  DCPower?  = null
    protected var tMeter:   TMeter?   = null
    protected var fControl: FControl? = null
    protected var tControl: TC?       = null
    protected var fpp1:     VMeter?   = null
    protected var fpp2:     VMeter?   = null
    protected var tvMeter:  VMeter?   = null
    protected var heater:   SMU?      = null

    abstract fun createPlot(data: ResultTable): Plot

    abstract fun processResults(data: ResultTable, extra: List<Quantity>): ResultFile

    /**
     * Checks that everything required for this measurement is present. Returns all missing instrument errors as
     * a list of strings. Measurement will only go ahead if this list is empty.
     */
    open fun checkForErrors() : List<String> = emptyList()

    open fun loadInstruments() {
        setters.forEach { it() }
    }

    override fun start() {

        errors.clear()
        loadInstruments()

        errors += checkForErrors()

        if (errors.isNotEmpty()) {
            throw Exception(errors.joinToString(", "))
        }

        results.setAttribute("Type", type)

        super.start()

    }

    override fun getName(): String {
        return this.name
    }

    override fun getLabel(): String {
        return labelProperty.value
    }

    override fun setLabel(value: String) {
        labelProperty.value = value
    }

    fun <I: Instrument> addOptionalInstrument(name: String, type: KClass<I>, setter: (I?) -> Unit = {}): Configuration<I> {
        val config = addInstrument(name, type.java)
        setters.add { setter(config.get()) }
        return config
    }

    fun <I: Instrument> addRequiredInstrument(name: String, type: KClass<I>, setter: (I) -> Unit = {}): Configuration<I> {

        val config = addInstrument(name, type.java)

        setters.add {

            val inst = config.instrument

            if (inst == null) {
                errors += "$name is not configured"
            } else {
                setter(inst)
            }

        }

        return config

    }

    fun addParameter(section: String, name: String, defaultValue: String, setter: (String) -> Unit) : Parameter<String> {
        val param = StringParameter(section, name, null, defaultValue)
        setters += { setter(param.value) }
        return param
    }

    fun addParameter(section: String, name: String, defaultValue: Double, setter: (Double) -> Unit) : Parameter<Double> {
        val param =  DoubleParameter(section, name, null, defaultValue)
        setters += { setter(param.value) }
        return param
    }

    fun addParameter(section: String, name: String, defaultValue: Int, setter: (Int) -> Unit) : Parameter<Int> {
        val param =  IntegerParameter(section, name, null, defaultValue)
        setters += { setter(param.value) }
        return param
    }

    fun addParameter(section: String, name: String, defaultValue: Boolean, setter: (Boolean) -> Unit) : Parameter<Boolean> {
        val param =  BooleanParameter(section, name, null, defaultValue)
        setters += { setter(param.value) }
        return param
    }

    fun addChoiceParameter(section: String, name: String, vararg options: String, setter: (Int) -> Unit) : Parameter<Int> {
        val param = ChoiceParameter(section, name, 0, *options)
        setters += { setter(param.value) }
        return param
    }

    fun addParameter(section: String, name: String, defaultValue: Range<Double>, setter: (Range<Double>) -> Unit) : Parameter<Range<Double>> {
        val param =  RangeParameter(section, name, null, defaultValue)
        setters += { setter(param.value) }
        return param
    }

    fun runRegardless (toRun: () -> Unit) {

        try {
            toRun()
        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }

}

