package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runRegardless
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.experiment.Col
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.maths.Range

class SyncMeasurement : Measurement() {

    private var sdSMU: SMU?    = null
    private var sgSMU: SMU?    = null
    private var gdSMU: SMU?    = null
    private var fpp1:  VMeter? = null
    private var fpp2:  VMeter? = null
    private var tm:    TMeter? = null

    private var minVSD = 0.0
    private var maxVSD = 60.0
    private var numVSD = 7
    private var symVSD = true

    private var offset = 0.0

    private var intTime = 1.0 / 50.0
    private var delTime = 500

    /**
     * Configure the source-drain parameters
     */
    fun configureVoltages(start: Double, stop: Double, steps: Int, sym: Boolean): SyncMeasurement {
        minVSD = start
        maxVSD = stop
        numVSD = steps
        symVSD = sym
        return this
    }

    fun configureOffset(offset: Double) : SyncMeasurement {
        this.offset = offset
        return this
    }

    fun configureTimes(integration: Double, delay: Double): SyncMeasurement {
        intTime = integration
        delTime = (delay * 1000).toInt()
        return this
    }

    fun loadInstruments(instruments: Instruments) {

        if (!instruments.hasSD || !instruments.hasSG) {
            throw Exception("Source-Drain and Source-Gate SMUs must be configured first")
        }

        sdSMU = instruments.sdSMU!!
        sgSMU = instruments.sgSMU!!
        gdSMU = instruments.gdSMU
        fpp1 = instruments.fpp1
        fpp2 = instruments.fpp2
        tm = instruments.tm

    }

    override fun run(results: ResultTable) {

        val sdSMU = this.sdSMU!!
        val sgSMU = this.sgSMU!!

        var voltages = Range.linear(minVSD, maxVSD, numVSD)

        if (symVSD) voltages = voltages.mirror()

        sdSMU.turnOff()
        sgSMU.turnOff()
        gdSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

        // Configure initial source modes
        sdSMU.voltage = minVSD
        sgSMU.voltage = minVSD + offset
        gdSMU?.voltage = 0.0

        // Configure integration times
        sdSMU.integrationTime = intTime
        sgSMU.integrationTime = intTime
        fpp1?.integrationTime = intTime
        fpp2?.integrationTime = intTime

        sdSMU.turnOn()
        sgSMU.turnOn()
        gdSMU?.turnOn()
        fpp1?.turnOn()
        fpp2?.turnOn()

        for (vSD in voltages) {

            val vSG = vSD + offset

            sdSMU.voltage = vSD
            sgSMU.voltage = vSG

            sleep(delTime)


            results.addData(
                vSD, vSG,
                sdSMU.voltage, sdSMU.current,
                sgSMU.voltage, sgSMU.current,
                fpp1?.voltage ?: Double.NaN, fpp2?.voltage ?: Double.NaN,
                tm?.temperature ?: Double.NaN,
                gdSMU?.current ?: Double.NaN
            )


        }

    }

    override fun onFinish() {

        runRegardless { sdSMU?.turnOff() }
        runRegardless { sgSMU?.turnOff() }
        runRegardless { gdSMU?.turnOff() }
        runRegardless { fpp1?.turnOff() }
        runRegardless { fpp2?.turnOff() }

    }

    override fun getName(): String = "Synced Voltage Measurement"

    override fun getColumns(): Array<Col> {

        return arrayOf(
            Col("Set SD Voltage", "V"),
            Col("Set SG Voltage", "V"),
            Col("SD Voltage", "V"),
            Col("SD Current", "A"),
            Col("SG Voltage", "V"),
            Col("SG Current", "A"),
            Col("Four Point Probe 1", "V"),
            Col("Four Point Probe 2", "V"),
            Col("Temperature", "K"),
            Col("Ground Current", "A")
        )

    }

    override fun onInterrupt() {

        Util.errLog.println("Synced voltage measurement interrupted.")

    }

}