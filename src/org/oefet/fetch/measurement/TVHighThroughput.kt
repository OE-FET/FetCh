package org.oefet.fetch.measurement

import jisa.Util.runInParallel
import jisa.control.Repeat
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TC
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.enums.AMode
import jisa.maths.Range
import jisa.results.Column
import jisa.results.DoubleColumn
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.TVHTPlot
import org.oefet.fetch.results.TVHighThroughputResult

class TVHighThroughput : FetChMeasurement("Thermal Voltage High Throughput", "TVThroughput", "Thermal Voltage High Throughput") {

    private val repTime  by userInput("Basic", "Repeat Time [s]", 0.0) map { (it * 1e3).toInt() }
    private val repeats  by userInput("Basic", "Repeats", 50)

    private val pctMarginPeltiers by userInput("Temperature Stabilization: Peltiers", "Percentage range for temperature to stay within",0.3 )
    private val durationPeltiers  by userInput("Temperature Stabilization: Peltiers", "Duration of temperature stabilization [s]",60.0 ) map { (it * 1e3).toLong() }
    private val maxTimePeltiers   by userInput("Temperature Stabilization: Peltiers", "Maximum Duration of temperature stabilization [s]",180.0 ) map { (it * 1e3).toLong() }

    private val pctMarginOnChip by userInput("Temperature Stabilization: On chip", "Percentage range for temperature to stay within",1.0 )
    private val durationOnChip  by userInput("Temperature Stabilization: On chip", "Duration of temperature stabilization [s]",10 ) map { (it * 1e3).toLong() }
    private val maxTimeOnChip   by userInput("Temperature Stabilization: On chip", "Maximum Duration of temperature stabilization [s]",180.0 ) map { (it * 1e3).toLong() }


    private val coldTemps by userInput("Temperature", "Cold Side Temperature [K]", Range.linear(295.15, 274, 3))
    private val hotTemps  by userInput("Temperature", "Hot Side Temperature [K]", Range.linear(295.15, 312, 3))

    // Instruments
    private val vMeter1 by requiredInstrument("Thermal Voltage Meter 1", VMeter::class)
    private val vMeter2 by optionalInstrument("Thermal Voltage Meter 2", VMeter::class)
    private val ground  by optionalInstrument("Ground", SMU::class)

    private val tMeter1 by requiredInstrument("Thermometer Cold", TMeter::class)
    private val tMeter2 by requiredInstrument("Thermometer Hot", TMeter::class)

    private val hotPeltier  by requiredInstrument("Hot Peltier", TC::class)
    private val coldPeltier by requiredInstrument("Cold Peltier", TC::class)




    companion object {
        val VOLTAGE                      = DoubleColumn("Voltage", "V")
        val VOLTAGE_ERROR                = DoubleColumn("Voltage Std. Deviation", "V")
        val TEMPERATURE1                 = DoubleColumn("Temperature 1", "K")
        val TEMPERATURE2                 = DoubleColumn("Temperature 2", "K")
        val TEMPERATURE_DIFFERENCE       = DoubleColumn("Temperature Difference", "K")
        val TEMPERATURE_DIFFERENCE_ERROR = DoubleColumn("Temperature Difference Std. Deviation", "K")

    }

    override fun processResults(data: ResultTable): TVHighThroughputResult {
        print("processResult")
        return TVHighThroughputResult(data)
    }

    override fun getColumns(): Array<Column<*>> {

        return arrayOf(
            VOLTAGE,
            VOLTAGE_ERROR,
            TEMPERATURE1,
            TEMPERATURE2,
            TEMPERATURE_DIFFERENCE,
            TEMPERATURE_DIFFERENCE_ERROR
        )

    }

    override fun run(results: ResultTable) {
        if(coldTemps.size() != hotTemps.size()){
            throw Exception ("Temperature Arrays have different size")
        }

        val list = ArrayList<Temperature>()
        for (i in 0 until coldTemps.size()) {
                list += Temperature(coldTemps[i],hotTemps[i])
        }

        // Record measurement parameters into result file
        results.setAttribute("Cold Temperatures", "$coldTemps K")
        results.setAttribute("Hot Temperatures", "$hotTemps K")


        // Start with everything turned-off

        vMeter1.turnOff()
        vMeter2?.turnOff()
        ground?.turnOff()
        print("turn off")

        // We don't want the instruments to do any averaging - we're doing that ourselves
        vMeter1.averageMode = AMode.NONE
        vMeter2?.averageMode = AMode.NONE

        ground?.voltage = 0.0

        vMeter1.turnOn()
        vMeter2?.turnOn()
        ground?.turnOn()

        val determineVoltage: (Double, Double, Double) -> Double = when {

            vMeter2 != null -> { vmeter1, vmeter2, _ -> vmeter2 - vmeter1 }
            ground != null  -> { vmeter1, _, ground  -> vmeter1 - ground }
            else            -> { vmeter1, _, _  -> vmeter1 }

        }

        val determineVoltageStdDeviation: (Double, Double, Double) -> Double = when {
            vMeter2 != null -> { vmeter1, vMeter2, _ -> vmeter1 + vMeter2 }
            ground != null  -> { vmeter1, _, ground  -> vmeter1 + ground }
            else            -> { vmeter1, _, _  -> vmeter1 }

        }

        for(temperatures in list){

            coldPeltier.temperature = temperatures.cold
            hotPeltier.temperature  = temperatures.hot

            coldPeltier.useAutoHeater()
            hotPeltier.useAutoHeater()

            runInParallel(
                { coldPeltier.waitForStableTemperatureMaxTime(temperatures.cold, pctMarginPeltiers, durationPeltiers,maxTimePeltiers) },
                { hotPeltier.waitForStableTemperatureMaxTime(temperatures.hot, pctMarginPeltiers, durationPeltiers, maxTimePeltiers) },
                { tMeter1.waitForStableTemperatureMaxTime(pctMarginOnChip, durationOnChip, maxTimeOnChip) },
                { tMeter2.waitForStableTemperatureMaxTime(pctMarginOnChip, durationOnChip, maxTimeOnChip) }
            )

            val vMeter1Values = Repeat.prepare(repeats, repTime) { vMeter1.voltage }
            val vMeter2Values = Repeat.prepare(repeats, repTime) { vMeter2?.voltage ?: Double.NaN}
            val tMeter1Values = Repeat.prepare(repeats, repTime) { tMeter1.temperature }
            val tMeter2Values = Repeat.prepare(repeats, repTime) { tMeter2.temperature }
            val tDiff         = Repeat.prepare(repeats, repTime) { tMeter2.temperature - tMeter1.temperature }

            Repeat.runTogether(vMeter1Values,vMeter2Values,tMeter1Values,tMeter2Values, tDiff)

            results.mapRow(
                VOLTAGE                      to determineVoltage(vMeter1Values.mean, vMeter2Values.mean,ground?.voltage  ?: Double.NaN),
                VOLTAGE_ERROR                to determineVoltageStdDeviation(vMeter1Values.standardDeviation, vMeter2Values.standardDeviation,ground?.voltage  ?: Double.NaN),
                TEMPERATURE1                 to tMeter1Values.mean,
                TEMPERATURE2                 to tMeter2Values.mean,
                TEMPERATURE_DIFFERENCE       to tDiff.mean,
                TEMPERATURE_DIFFERENCE_ERROR to tDiff.standardDeviation
            )

        }






    }

    override fun onFinish() {

        runRegardless(
            { vMeter1.turnOff() },
            { vMeter2?.turnOff() },
            { ground?.turnOff() },
            { hotPeltier.setHeaterPower(0.0) },
            { coldPeltier.setHeaterPower(0.0) }
        )

    }


    override fun createDisplay(data: ResultTable): TVHTPlot {
        return TVHTPlot(data)
    }

    class Temperature(val cold: Double, val hot: Double)




}