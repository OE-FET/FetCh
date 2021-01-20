package org.oefet.fetch.measurement

import jisa.devices.*
import jisa.experiment.Measurement
import kotlin.reflect.KClass

abstract class FMeasurement(private val name: String, private val label: String, val type: String) : Measurement() {

    private val labelProperty = StringParameter("Basic", "Name", null, label);

    protected var gdSMU: SMU?         = null
    protected var sdSMU: SMU?         = null
    protected var sgSMU: SMU?         = null
    protected var lockIn: DPLockIn?   = null
    protected var preAmp: VPreAmp?    = null
    protected var dcPower: DCPower?   = null
    protected var tMeter: TMeter?     = null
    protected var fControl: FControl? = null
    protected var tControl: TC?       = null
    protected var fpp1: VMeter?       = null
    protected var fpp2: VMeter?       = null
    protected var tvMeter: VMeter?    = null
    protected var heater: SMU?        = null

    /**
     * Checks that everything required for this measurement is present. Returns all missing instrument errors as
     * a list of strings. Measurement will only go ahead if this list is empty.
     */
    abstract fun checkForErrors() : List<String>

    abstract fun loadInstruments()

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

    fun <I: Instrument> addInstrument(name: String, type: KClass<I>): Configuration<I> {
        return addInstrument(name, type.java)
    }

}

