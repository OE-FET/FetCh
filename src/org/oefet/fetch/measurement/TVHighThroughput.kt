package org.oefet.fetch.measurement

import jisa.Util
import jisa.control.Repeat
import jisa.devices.interfaces.IMeter
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.enums.AMode
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementSubAction
import jisa.maths.Range
import org.oefet.fetch.gui.elements.TVPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.TVHighThroughputResult
import org.oefet.fetch.results.TVResult

class TVHighThroughput : FetChMeasurement("Thermal Voltage High Throughput", "TVThroughput", "Thermal Voltage High Throughput") {

    private val pctMargin by input("Temperature Stabilization", "Percentage range for temperature to stay within",0.3, )
    private val duration by input("Temperature Stabilization", "Duration of temperature stabilization [s]",60.0, ) map { (it * 1e3).toLong() }

    // Instruments
    private val vMeter by requiredConfig("Thermal Voltage Meter", VMeter::class)
    private val tMeter1  by requiredConfig("Thermometer 1", TMeter::class)
    private val tMeter2  by requiredConfig("Thermometer 2", TMeter::class)

    companion object {
        val VOLTAGE     = Col("Voltage ", "V")
        val TEMPERATURE1    = Col("Temperature 1", "K")
        val TEMPERATURE2    = Col("Temperature 2", "K")

    }

    override fun processResults(data: ResultTable, extra: List<Quantity>): TVHighThroughputResult {
        return TVHighThroughputResult(data, extra)

    }

    override fun getColumns(): Array<Col> {

        return arrayOf(
            VOLTAGE,
            TEMPERATURE1,
            TEMPERATURE2
        )

    }

    override fun run(results: ResultTable) {

        // Record measurement parameters into result file
        results.setAttribute("Hot Peltier set temperature", "s")

        // Start with everything turned-off
        vMeter.turnOff()

        // We don't want the instruments to do any averaging - we're doing that ourselves
        vMeter.averageMode = AMode.NONE


        tMeter1.waitForStableTemperature(pctMargin,duration)
        tMeter2.waitForStableTemperature(pctMargin,duration)
        vMeter.turnOn()

        results.addData(
            vMeter.voltage,
            tMeter1.temperature,
            tMeter2.temperature
        )
    }

    override fun onFinish() {
        runRegardless { vMeter.turnOff() }
    }


}