package org.oefet.fetch.measurement

import jisa.Util
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.experiment.Col
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import org.oefet.fetch.gui.Configuration

class VoltageHold : Measurement() {

    private var sdSMU: SMU?   = null
    private var sgSMU: SMU?   = null

    private var holdSD   = false
    private var holdSG   = false
    private var sdV      = 0.0
    private var sgV      = 0.0
    private var holdTime = 100

    fun loadInstruments() {

        val instruments = Configuration.getInstruments()

        if (!instruments.hasSD || !instruments.hasSG) {
            throw Exception("Source-Drain and Source-Gate SMUs must be configured first")
        }

        sdSMU = instruments.sdSMU!!
        sgSMU = instruments.sgSMU!!

    }

    fun configureVoltages(holdSD: Boolean, sdV: Double, holdSG: Boolean, sgV: Double) : VoltageHold {

        this.holdSD = holdSD
        this.sdV    = sdV
        this.holdSG = holdSG
        this.sgV    = sgV

        return this

    }

    fun configureTime(holdTime: Double) : VoltageHold {

        this.holdTime = (1000.0 * holdTime).toInt()

        return this

    }

    override fun run(results: ResultTable?) {

        loadInstruments()

        val sdSMU = this.sdSMU!!
        val sgSMU = this.sgSMU!!

        if (holdSD) {
            sdSMU.voltage = sdV
            sdSMU.turnOn()
        }

        if (holdSG) {
            sgSMU.voltage = sgV
            sgSMU.turnOn()
        }

        sleep(holdTime)

    }

    override fun onFinish() {

        sdSMU?.turnOff()
        sgSMU?.turnOff()

    }

    override fun getName(): String {
        return "Voltage Hold" + (if (holdSD) " SD = $sdV V" else "") + (if (holdSG) " SG = $sgV V" else "")
    }

    override fun getColumns(): Array<Col> {
        return emptyArray()
    }

    override fun onInterrupt() {
        Util.errLog.println("Voltage hold interrupted")
    }

}