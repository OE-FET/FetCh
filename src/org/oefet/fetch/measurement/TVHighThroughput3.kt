package org.oefet.fetch.measurement

import jisa.Util.runInParallel
import jisa.control.Repeat
import jisa.devices.meter.TMeter
import jisa.devices.meter.VMeter
import jisa.devices.smu.SMU
import jisa.enums.AMode
import jisa.maths.Range
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.TVHTPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.results.TVHighThroughputResult

class TVHighThroughput3 : FetChMeasurement("Thermal Voltage High Throughput With Voltage", "TVThroughput", "Thermal Voltage High Throughput", Images.getImage("fire.png")) {

    private val repTime       by userTimeInput("Basic",   "Repeat Time", 0)
    private val repeats       by userInput    ("Basic",   "Repeats", 50)
    private val biasVoltages1 by userInput    ("Voltage", "Set Voltages [V]", Range.linear(0.0, 1.0, 3))
    private val pctStabChip   by userInput("Chip Stabilisation", "Stays within [%]", 1.0)
    private val durStabChip   by userTimeInput("Chip Stabilisation", "For at least", 10000) map { it.toLong() }
    private val tmoStabChip   by userTimeInput("Chip Stabilisation", "Timeout", 180000) map { it.toLong() }

    // Instruments
    private val vMeter1     by requiredInstrument("Thermal Voltage Meter", VMeter::class)
    private val biasSource1 by requiredInstrument("Bias Source (SMU)", SMU::class)
    private val tMeter1     by requiredInstrument("Thermometer Cold", TMeter::class)
    private val tMeter2     by requiredInstrument("Thermometer Hot", TMeter::class)


    // Data columns
    companion object : Columns() {

        val VOLTAGE                      = decimalColumn("Voltage", "V")
        val VOLTAGE_ERROR                = decimalColumn("Voltage Std. Deviation", "V")
        val TEMPERATURE1                 = decimalColumn("Temperature 1", "K")
        val TEMPERATURE2                 = decimalColumn("Temperature 2", "K")
        val TEMPERATURE_DIFFERENCE       = decimalColumn("Temperature Difference", "K")
        val TEMPERATURE_DIFFERENCE_ERROR = decimalColumn("Temperature Difference Error", "K")

    }

    override fun processResults(data: ResultTable): TVHighThroughputResult {
        return TVHighThroughputResult(data)
    }

    override fun run(results: ResultTable) {

        // Record measurement parameters into result file
        results.setAttribute("Set Voltages [V]", "$biasVoltages1 V")

        // Start with everything turned off
        vMeter1.turnOff()
        biasSource1.turnOff()
        print("turn off")

        // We don't want the instruments to do any averaging - we're doing that ourselves
        vMeter1.averageMode = AMode.NONE

        vMeter1.turnOn()
        biasSource1.turnOn()

        for (vSet in biasVoltages1) {

            biasSource1.voltage = vSet

            runInParallel(
                { tMeter1.waitForStableTemperature(pctStabChip, durStabChip, tmoStabChip) },
                { tMeter2.waitForStableTemperature(pctStabChip, durStabChip, tmoStabChip) }
            )

            val v1 = Repeat.prepare(repeats, repTime) { vMeter1.voltage }
            val t1 = Repeat.prepare(repeats, repTime) { tMeter1.temperature }
            val t2 = Repeat.prepare(repeats, repTime) { tMeter2.temperature }
            val tD = Repeat.prepare(repeats, repTime) { tMeter2.temperature - tMeter1.temperature }

            Repeat.runTogether(v1, t1, t2, tD)

            results.mapRow(
                VOLTAGE                      to v1.mean,
                VOLTAGE_ERROR                to v1.standardDeviation,
                TEMPERATURE1                 to t1.mean,
                TEMPERATURE2                 to t2.mean,
                TEMPERATURE_DIFFERENCE       to tD.mean,
                TEMPERATURE_DIFFERENCE_ERROR to tD.standardDeviation
            )

        }

    }

    override fun onFinish() {

        runRegardless(
            { vMeter1.turnOff() },
            { biasSource1.apply { voltage = 0.0; turnOff() } }
        )

    }

    override fun createDisplay(data: ResultTable): TVHTPlot {
        return TVHTPlot(data)
    }

}