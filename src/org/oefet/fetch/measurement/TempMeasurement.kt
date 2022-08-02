package org.oefet.fetch.measurement

import jisa.control.Repeat
import jisa.devices.interfaces.TMeter
import jisa.enums.Icon
import jisa.results.Col
import jisa.results.ResultTable

class TempMeasurement : FetChMeasurement("Temperature Measurement", "Temp", "Temp", Icon.THERMOMETER.blackImage) {

    private val pctMargin by userInput("Temperature Stabilization", "Percentage range for temperature to stay within",0.3, )
    private val duration by userInput("Temperature Stabilization", "Duration of temperature stabilization [s]",60.0, ) map { (it * 1e3).toLong() }
    private val maxTime by userInput("Temperature Stabilization", "Maximum Duration of temperature stabilization [s]",180.0 ) map { (it * 1e3).toLong() }

    private val repTime  by userInput("Basic", "Repeat Time [s]", 0.0) map { (it * 1e3).toInt() }
    private val repeats  by userInput("Basic", "Repeats", 50)

    private val tMeter by requiredInstrument("Thermometer", TMeter::class)

    companion object{
        val TEMPERATURE = Col("Temperature", "K")
        val TEMPERATURE_STD = Col("Temp. Std. Dev.", "K")
    }

    override fun getColumns(): Array<Col> {


        return arrayOf(
            TEMPERATURE,
            TEMPERATURE_STD
        )
    }
    override fun run(results: ResultTable) {
        tMeter.waitForStableTemperatureMaxTime(pctMargin,duration,maxTime)

        val tMeterValues = Repeat.prepare(repeats, repTime) { tMeter.temperature }
        Repeat.runTogether(tMeterValues)

        results.addData(
            tMeterValues.mean,
            tMeterValues.standardDeviation
        )

    }

    override fun onFinish() {
    }
}

