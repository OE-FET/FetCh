package org.oefet.fetch.measurement

import jisa.devices.interfaces.*
import jisa.devices.Configuration
import jisa.experiment.Measurement
import java.util.*
import kotlin.reflect.KClass

abstract class FMeasurement(private val name: String, private val label: String, val type: String) : Measurement() {

    private val labelProperty     = StringParameter("Basic", "Name", null, label)
    private val instrumentSetters = LinkedList<() -> Unit>()

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

    /**
     * Checks that everything required for this measurement is present. Returns all missing instrument errors as
     * a list of strings. Measurement will only go ahead if this list is empty.
     */
    abstract fun checkForErrors() : List<String>

    open fun loadInstruments() {
        instrumentSetters.forEach { it() }
    }

    override fun start() {

        loadInstruments()

        val errors = checkForErrors()

        if (errors.isNotEmpty()) {
            throw Exception(errors.joinToString(", "))
        }

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

    fun <I: Instrument> addInstrument(name: String, type: KClass<I>, setter: (I?) -> Unit = {}): Configuration<I> {
        val config = addInstrument(name, type.java)
        instrumentSetters.add { setter(config.get()) }
        return config
    }

    fun runRegardless (toRun: () -> Unit) {

        try {
            toRun()
        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }

}

