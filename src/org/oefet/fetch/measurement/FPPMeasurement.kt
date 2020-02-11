package org.oefet.fetch.measurement

import jisa.Util.runRegardless
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.experiment.Col
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.maths.Range
import java.time.Duration
import kotlin.math.abs

class FPPMeasurement : Measurement() {

    private var gdSMU : SMU?    = null
    private var sdSMU : SMU?    = null
    private var fpp1  : VMeter? = null
    private var fpp2  : VMeter? = null
    private var tm    : TMeter? = null

    private var minI    = 0.0
    private var maxI    = 10e-6
    private var numI    = 11
    private var symI     = false
    private var intTime = 20e-3
    private var delTime   = Duration.ofSeconds(1).toMillis()

    fun loadInstruments(instruments: Instruments) {

        sdSMU = instruments.sdSMU
        gdSMU = instruments.gdSMU
        fpp1  = instruments.fpp1
        fpp2  = instruments.fpp2
        tm    = instruments.tm

    }

    fun configureCurrent(minI: Double, maxI: Double, numI: Int, symI: Boolean) : FPPMeasurement {
        this.minI = minI
        this.maxI = maxI
        this.numI = numI
        this.symI = symI
        return this
    }

    fun configureTiming(intTime: Double, delTime: Double) : FPPMeasurement {
        this.intTime = intTime
        this.delTime = (delTime * 1000.0).toLong()
        return this
    }

    override fun run(results: ResultTable) {

        val sdSMU = sdSMU
        val fpp1  = fpp1

        if (sdSMU == null || fpp1 == null) throw Exception("SD and FPP1 channels are not configured")

        // Turn everything off before starting
        gdSMU?.turnOff()
        sdSMU.turnOff()
        fpp1.turnOff()
        fpp2?.turnOff()

        gdSMU?.integrationTime = intTime
        sdSMU.integrationTime  = intTime
        fpp1.integrationTime   = intTime
        fpp2?.integrationTime  = intTime

        // Configure all channel voltages/currents
        gdSMU?.voltage = 0.0
        sdSMU.current = minI

        // Enable all channels
        gdSMU?.turnOn()
        sdSMU.turnOn()
        fpp1.turnOn()
        fpp2?.turnOn()

        // Sweep current
        for (current in if (symI) Range.linear(minI, maxI, numI).mirror() else Range.linear(minI, maxI, numI)) {

            sdSMU.current = current
            sleep(delTime)

            results.addData(
                sdSMU.current,
                sdSMU.voltage,
                fpp1.voltage,
                fpp2?.voltage ?: 0.0,
                tm?.temperature ?: 0.0,
                gdSMU?.current ?: 0.0
            )

        }

    }

    private fun sleep(millis: Long) = sleep(millis.toInt())

    override fun onFinish() {

        runRegardless { gdSMU?.turnOff() }
        runRegardless { sdSMU?.turnOff() }
        runRegardless { fpp1?.turnOff() }
        runRegardless { fpp2?.turnOff() }

    }

    override fun getName(): String {
        return "4PP Conductivity Measurement"
    }

    override fun getColumns(): Array<Col> = arrayOf(
        Col("SD Current", "A"),
        Col("SD Voltage", "V"),
        Col("FPP 1 Voltage", "V"),
        Col("FPP 2 Voltage", "V"),
        Col("FPP Voltage", "V") { abs( it[FPP2_VOLTAGE] - it[FPP1_VOLTAGE]) },
        Col("Temperature", "K"),
        Col("Ground Current", "A")
    )

    override fun onInterrupt() {}

    companion object {
        const val SD_CURRENT     = 0
        const val SD_VOLTAGE     = 1
        const val FPP1_VOLTAGE   = 2
        const val FPP2_VOLTAGE   = 3
        const val FPP_VOLTAGE    = 4
        const val TEMPERATURE    = 5
        const val GROUND_CURRENT = 6;
    }

}