package org.oefet.fetch.measurement

import jisa.control.RTask
import jisa.devices.*
import jisa.experiment.Col
import jisa.experiment.ResultStream
import jisa.experiment.ResultTable
import org.oefet.fetch.gui.tabs.Dashboard

object Log {

    const val TIME_MS         = 0
    const val GROUND_VOLTAGE  = 1
    const val GROUND_CURRENT  = 2
    const val SD_VOLTAGE      = 3
    const val SD_CURRENT      = 4
    const val SG_VOLTAGE      = 5
    const val SG_CURRENT      = 6
    const val FPP1_VOLTAGE    = 7
    const val FPP2_VOLTAGE    = 8
    const val HEATER_VOLTAGE  = 9
    const val HEATER_CURRENT  = 10
    const val HEATER_POWER    = 11
    const val THERMAL_VOLTAGE = 12
    const val FREQUENCY       = 13
    const val X_VOLTAGE       = 14
    const val Y_VOLTAGE       = 15
    const val TEMPERATURE     = 16
    const val TC_HEATER_POWER = 17

    val COLUMNS =  arrayOf(
        Col("Running Time", "s"),
        Col("Ground Voltage", "V"),
        Col("Ground Current", "A"),
        Col("Source-Drain Voltage", "V"),
        Col("Source-Drain Current", "A"),
        Col("Source-Gate Voltage", "V"),
        Col("Source-Gate Current", "A"),
        Col("FPP 1 Voltage", "V"),
        Col("FPP 2 Voltage", "V"),
        Col("Heater Voltage", "V"),
        Col("Heater Current", "A"),
        Col("Heater Power", "W") { it[HEATER_VOLTAGE] * it[HEATER_CURRENT] },
        Col("Thermal Voltage", "V"),
        Col("Field Frequency", "Hz"),
        Col("X Voltage", "V"),
        Col("Y Voltage", "V"),
        Col("Temperature", "K"),
        Col("TC Heater Power", "%")
    )

    private var gdSMU: SMU?         = null
    private var sdSMU: SMU?         = null
    private var sgSMU: SMU?         = null
    private var lockIn: DPLockIn?   = null
    private var preAmp: VPreAmp?    = null
    private var dcPower: DCPower?   = null
    private var tMeter: TMeter?     = null
    private var fControl: FControl? = null
    private var tControl: TC?       = null
    private var fpp1: VMeter?       = null
    private var fpp2: VMeter?       = null
    private var tvMeter: VMeter?    = null
    private var heater: SMU?        = null

    private var log    : ResultTable? = null;
    private val logger : RTask        = RTask(2500) { it -> log(log!!, it) }

    fun start(path: String) {

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

        logger.stop()
        log = ResultStream(path, *COLUMNS)
        logger.start()

        Dashboard.watchLog(log!!)

    }

    fun stop() {

        logger.stop()
        log?.finalise()

    }

    private fun log(log: ResultTable, task: RTask) {

        log.addData(
            task.secFromStart,
            gdSMU?.voltage        ?: 0.0,
            gdSMU?.current        ?: 0.0,
            sdSMU?.voltage        ?: 0.0,
            sdSMU?.current        ?: 0.0,
            sgSMU?.voltage        ?: 0.0,
            sgSMU?.current        ?: 0.0,
            fpp1?.voltage         ?: 0.0,
            fpp2?.voltage         ?: 0.0,
            heater?.voltage       ?: 0.0,
            heater?.current       ?: 0.0,
            tvMeter?.voltage      ?: 0.0,
            lockIn?.frequency     ?: 0.0,
            lockIn?.lockedX       ?: 0.0,
            lockIn?.lockedY       ?: 0.0,
            tMeter?.temperature   ?: 0.0,
            tControl?.heaterPower ?: 0.0
        )

    }

}