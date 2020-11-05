package org.oefet.fetch.measurement

import jisa.devices.*
import jisa.experiment.Measurement

abstract class FMeasurement : Measurement() {

    abstract val type: String

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

    protected open fun loadInstruments() {

        gdSMU    = Instruments.gdSMU
        sdSMU    = Instruments.sdSMU
        sgSMU    = Instruments.sgSMU
        heater   = Instruments.htSMU
        tvMeter  = Instruments.tvMeter
        fpp1     = Instruments.fpp1
        fpp2     = Instruments.fpp2
        tControl = Instruments.tControl
        tMeter   = Instruments.tMeter
        lockIn   = Instruments.lockIn
        dcPower  = Instruments.dcPower
        preAmp   = Instruments.preAmp
        fControl = if (lockIn != null && dcPower != null) FControl(lockIn!!, dcPower!!) else null

    }

    override fun start() {

        loadInstruments()

        val errors = checkForErrors()

        if (errors.isNotEmpty()) {
            throw Exception(errors.joinToString(", "))
        }

        super.start()

    }

}

