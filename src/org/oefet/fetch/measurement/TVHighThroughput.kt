package org.oefet.fetch.measurement

import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.enums.AMode
import jisa.results.Column
import jisa.results.DoubleColumn
import jisa.results.ResultTable
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.TVHighThroughputResult

class TVHighThroughput : FetChMeasurement("Thermal Voltage High Throughput", "TVThroughput", "Thermal Voltage High Throughput") {

    private val pctMargin by input("Temperature Stabilization", "Percentage range for temperature to stay within",0.3 )
    private val duration by input("Temperature Stabilization", "Duration of temperature stabilization [s]",60.0 ) map { (it * 1e3).toLong() }

    // Instruments
    private val vMeter1   by requiredConfig("Thermal Voltage Meter 1", VMeter::class)
    private val vMeter2   by requiredConfig("Thermal Voltage Meter 2", VMeter::class)
    private val ground   by optionalConfig("Ground", SMU::class)

    private val tMeter1  by requiredConfig("Thermometer 1", TMeter::class)
    private val tMeter2  by requiredConfig("Thermometer 2", TMeter::class)

    companion object {
        val VOLTAGE      = DoubleColumn("Voltage", "V")
        val TEMPERATURE1 = DoubleColumn("Temperature 1", "K")
        val TEMPERATURE2 = DoubleColumn("Temperature 2", "K")

    }

    override fun processResults(data: ResultTable, extra: List<Quantity>): TVHighThroughputResult {
        print("processResult")
        return TVHighThroughputResult(data, extra)
    }

    override fun getColumns(): Array<Column<*>> {

        return arrayOf(
            VOLTAGE,
            TEMPERATURE1,
            TEMPERATURE2
        )

    }

    override fun run(results: ResultTable) {

        // Record measurement parameters into result file
        //results.setAttribute("Hot Peltier set temperature")

        // Start with everything turned-off

        vMeter1.turnOff()
        vMeter2.turnOff()
        ground?.turnOff()
        print("turn off")

        // We don't want the instruments to do any averaging - we're doing that ourselves
        vMeter1.averageMode = AMode.NONE
        vMeter2.averageMode = AMode.NONE
        print("no averaging")

        ground?.voltage = 0.0
        print("setground")

        tMeter1.waitForStableTemperature(pctMargin,duration)
        tMeter2.waitForStableTemperature(pctMargin,duration)
        print("waiting done")
        vMeter1.turnOn()
        vMeter2.turnOn()
        ground?.turnOn()

        results.addData(
            vMeter1.voltage - vMeter2.voltage,
            tMeter1.temperature,
            tMeter2.temperature
        )
        print("added data")
    }

    override fun onFinish() {
        runRegardless { vMeter1.turnOff() }
        runRegardless { vMeter2.turnOff() }
    }


}