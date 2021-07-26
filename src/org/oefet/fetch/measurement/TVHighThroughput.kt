package org.oefet.fetch.measurement

import jisa.Util
import jisa.Util.runInParallel
import jisa.control.Repeat
import jisa.control.Returnable
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
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.TVHighThroughputResult

class TVHighThroughput : FetChMeasurement("Thermal Voltage High Throughput", "TVThroughput", "Thermal Voltage High Throughput") {

    private val repTime  by input("Basic", "Repeat Time [s]", 0.0) map { (it * 1e3).toInt() }
    private val repeats  by input("Basic", "Repeats", 50)

    private val pctMarginPeltiers by input("Temperature Stabilization: Peltiers", "Percentage range for temperature to stay within",0.3 )
    private val durationPeltiers by input("Temperature Stabilization: Peltiers", "Duration of temperature stabilization [s]",60.0 ) map { (it * 1e3).toLong() }
    private val maxTimePeltiers by input("Temperature Stabilization: Peltiers", "Maximum Duration of temperature stabilization [s]",180.0 ) map { (it * 1e3).toLong() }

    private val pctMarginOnChip by input("Temperature Stabilization: On chip", "Percentage range for temperature to stay within",1.0 )
    private val durationOnChip by input("Temperature Stabilization: On chip", "Duration of temperature stabilization [s]",10 ) map { (it * 1e3).toLong() }
    private val maxTimeOnChip by input("Temperature Stabilization: On chip", "Maximum Duration of temperature stabilization [s]",180.0 ) map { (it * 1e3).toLong() }


    private val coldTemps by input("Temperature", "Cold Side Temperature [K]", Range.linear(295.15, 274, 3))
    private val hotTemps by input("Temperature", "Hot Side Temperature [K]", Range.linear(295.15, 312, 3))

    // Instruments
    private val vMeter1   by requiredConfig("Thermal Voltage Meter 1", VMeter::class)
    private val vMeter2   by optionalConfig("Thermal Voltage Meter 2", VMeter::class)
    private val ground   by optionalConfig("Ground", SMU::class)

    private val tMeter1  by requiredConfig("Thermometer Cold", TMeter::class)
    private val tMeter2  by requiredConfig("Thermometer Hot", TMeter::class)

    private val hotPeltier  by requiredConfig("Hot Peltier", TC::class)
    private val coldPeltier  by requiredConfig("Cold Peltier", TC::class)




    companion object {
        val VOLTAGE      = DoubleColumn("Voltage", "V")
        val VOLTAGESTDDEVIATION = DoubleColumn("Voltage Std. Deviation", "V")
        val TEMPERATURE1 = DoubleColumn("Temperature 1", "K")
        val TEMPERATURE2 = DoubleColumn("Temperature 2", "K")
        val TEMPERATURE_DIFFERENCE = DoubleColumn("Temperature Difference", "K")
        val TEMPERATURE_DIFFERENCE_DISTR = DoubleColumn("Temperature Difference Std. Deviation", "K")

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
            TEMPERATURE2,
            TEMPERATURE_DIFFERENCE,
            TEMPERATURE_DIFFERENCE_DISTR
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

            vMeter2 != null              -> { vmeter1, vmeter2, _ -> vmeter2 - vmeter1 }
            ground != null              -> { vmeter1, _, ground  -> vmeter1 - ground }
            else                         -> { vmeter1, _, _  -> vmeter1 }

        }

        val determineVoltageStdDeviation: (Double, Double, Double) -> Double = when {
            vMeter2 != null              -> { vmeter1, vMeter2, _ -> vmeter1 + vMeter2 }
            ground != null              -> { vmeter1, _, ground  -> vmeter1 + ground }
            else                         -> { vmeter1, _, _  -> vmeter1 }

        }

        for(temperatures in list){

            coldPeltier.temperature = temperatures.cold
            hotPeltier.temperature = temperatures.hot
            coldPeltier.useAutoHeater()
            hotPeltier.useAutoHeater()

            //WaitForStableTemperatureMultiple(coldPeltier::getTemperature,hotPeltier::getTemperature,temperatures.cold,temperatures.hot,pctMarginPeltiers,durationPeltiers,maxTimePeltiers)
            //WaitForStableTemperatureMultiple(tMeter1::getTemperature,tMeter2::getTemperature,tMeter1.temperature,tMeter2.temperature ,pctMarginOnChip,durationOnChip,maxTimeOnChip)

            /*
            runInParallel(
                () -> (
                coldPeltier.waitForStableTemperature(temperatures.cold,pctMarginPeltiers,durationPeltiers),
                hotPeltier.waitForStableTemperature(temperatures.hot,pctMarginPeltiers,durationPeltiers),
                tMeter1.waitForStableTemperature(pctMarginOnChip,durationOnChip),
                tMeter1.waitForStableTemperature(pctMarginOnChip,durationOnChip),
            )

             */

            val vMeter1Values = Repeat.prepare(repeats, repTime) { vMeter1.voltage }
            val vMeter2Values = Repeat.prepare(repeats, repTime) { vMeter2?.voltage ?: Double.NaN}
            val tMeter1Values = Repeat.prepare(repeats, repTime) { tMeter1.temperature }
            val tMeter2Values = Repeat.prepare(repeats, repTime) { tMeter2.temperature }
            val tDiff         = Repeat.prepare(repeats, repTime) { tMeter2.temperature - tMeter1.temperature }

            Repeat.runTogether(vMeter1Values,vMeter2Values,tMeter1Values,tMeter2Values, tDiff)

            results.addData(
                determineVoltage(vMeter1Values.mean,vMeter2Values.mean,ground?.voltage  ?: Double.NaN),
                determineVoltageStdDeviation(vMeter1Values.standardDeviation,vMeter2Values.standardDeviation,ground?.voltage  ?: Double.NaN),
                tMeter1Values.mean,
                tMeter2Values.mean,
                tDiff.mean,
                tDiff.standardDeviation

            )

        }






    }

    override fun onFinish() {
        runRegardless { vMeter1.turnOff() }
        runRegardless { vMeter2?.turnOff() }
        runRegardless { ground?.turnOff() }
        runRegardless { hotPeltier.setHeaterPower(0.0) }
        runRegardless { coldPeltier.setHeaterPower(0.0) }

    }


/*
    fun WaitForStableTemperatureMultiple(valueToCheckTMeter1 : Returnable<Double> ,valueToCheckTMeter2 : Returnable<Double>,target1 : Double, target2 : Double,  pctMargin : Double, duration : Long, maxTime: Long){
        var time: Long = 0
        var absolutTime: Long = 0
        var min1: Double = target1 * (1 - pctMargin / 100.0)
        var max1: Double = target1 * (1 + pctMargin / 100.0)
        var min2: Double = target2 * (1 - pctMargin / 100.0)
        var max2: Double = target2 * (1 + pctMargin / 100.0)

        while (time < duration && absolutTime < maxTime)
        {
            if (Thread.currentThread().isInterrupted) {
                throw InterruptedException("Interrupted")
            }
            val valueTMeter1: Double = valueToCheckTMeter1.get()
            val valueTMeter2: Double = valueToCheckTMeter2.get()



            if (Util.isBetween(valueTMeter1, min1, max1) && Util.isBetween(valueTMeter2, min2, max2) ) {
                time += 1000
                absolutTime += 1000
            } else {
                time = 0
                absolutTime += 1000
            }
            Thread.sleep(1000)
        }

    }
*/

    override fun createPlot(data: ResultTable): TVHTPlot {
        return TVHTPlot(data)
    }

    class Temperature(val cold: Double, val hot: Double)




}