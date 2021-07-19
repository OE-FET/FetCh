package org.oefet.fetch.measurement

import jisa.control.Repeat
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

    private val repTime  by input("Basic", "Repeat Time [s]", 0.0) map { (it * 1e3).toInt() }
    private val repeats  by input("Basic", "Repeats", 50)

    private val pctMargin by input("Temperature Stabilization", "Percentage range for temperature to stay within",0.3 )
    private val duration by input("Temperature Stabilization", "Duration of temperature stabilization [s]",60.0 ) map { (it * 1e3).toLong() }

    // Instruments
    private val vMeter1   by requiredConfig("Thermal Voltage Meter 1", VMeter::class)
    private val vMeter2   by optionalConfig("Thermal Voltage Meter 2", VMeter::class)
    private val ground   by optionalConfig("Ground", SMU::class)

    private val tMeter1  by requiredConfig("Thermometer 1", TMeter::class)
    private val tMeter2  by requiredConfig("Thermometer 2", TMeter::class)

    companion object {
        val VOLTAGE      = DoubleColumn("Voltage", "V")
        val VOLTAGESTDDEVIATION = DoubleColumn("Voltage Std. Deviation", "V")
        val TEMPERATURE1 = DoubleColumn("Temperature 1", "K")
        val TEMPARTURE1STDDEVIATION = DoubleColumn("Temperature 1 Std. Dev.", "K")
        val TEMPERATURE2 = DoubleColumn("Temperature 2", "K")
        val TEMPARTURE2STDDEVIATION = DoubleColumn("Temperature 2 Std. Dev.", "K")

    }

    override fun processResults(data: ResultTable, extra: List<Quantity>): TVHighThroughputResult {
        print("processResult")
        return TVHighThroughputResult(data, extra)
    }

    override fun getColumns(): Array<Column<*>> {

        return arrayOf(
            VOLTAGE,
            VOLTAGESTDDEVIATION,
            TEMPERATURE1,
            TEMPARTURE1STDDEVIATION,
            TEMPERATURE2,
            TEMPARTURE2STDDEVIATION,
        )

    }

    override fun run(results: ResultTable) {

        // Record measurement parameters into result file
        //results.setAttribute("Hot Peltier set temperature")

        // Start with everything turned-off

        vMeter1.turnOff()
        vMeter2?.turnOff()
        ground?.turnOff()
        print("turn off")

        // We don't want the instruments to do any averaging - we're doing that ourselves
        vMeter1.averageMode = AMode.NONE
        vMeter2?.averageMode = AMode.NONE
        print("no averaging")

        ground?.voltage = 0.0
        print("setground")

        tMeter1.waitForStableTemperature(pctMargin,duration)
        tMeter2.waitForStableTemperature(pctMargin,duration)
        print("waiting done")
        vMeter1.turnOn()
        vMeter2?.turnOn()
        ground?.turnOn()

        val vMeter1Values = Repeat.prepare(repeats, repTime) { vMeter1.voltage }
        val vMeter2Values = Repeat.prepare(repeats, repTime) { vMeter2?.voltage ?: Double.NaN}
        val tMeter1Values = Repeat.prepare(repeats, repTime) { tMeter1.temperature }
        val tMeter2Values = Repeat.prepare(repeats, repTime) { tMeter1.temperature }

        Repeat.runTogether(vMeter1Values,vMeter2Values)


        val determineVoltage: (Double, Double, Double) -> Double = when {

            vMeter2 != null              -> { vmeter1, vMeter2, _ -> vmeter1 - vMeter2 }
            ground != null              -> { vmeter1, _, ground  -> vmeter1 - ground }
            else                         -> { vmeter1, _, _  -> vmeter1 }

        }

        val determineVoltageStdDeviation: (Double, Double, Double) -> Double = when {
            vMeter2 != null              -> { vmeter1, vMeter2, _ -> vmeter1 + vMeter2 }
            ground != null              -> { vmeter1, _, ground  -> vmeter1 + ground }
            else                         -> { vmeter1, _, _  -> vmeter1 }

        }

        results.addData(
            determineVoltage(vMeter1Values.mean,vMeter2Values.mean,ground?.voltage  ?: Double.NaN),
            determineVoltageStdDeviation(vMeter1Values.standardDeviation,vMeter2Values.standardDeviation,ground?.voltage  ?: Double.NaN),
            tMeter1Values.mean,
            tMeter1Values.standardDeviation,
            tMeter2Values.mean,
            tMeter2Values.standardDeviation
        )
        print("added data")
    }

    override fun onFinish() {
        runRegardless { vMeter1.turnOff() }
        runRegardless { vMeter2?.turnOff() }
    }


}