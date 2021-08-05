package org.oefet.fetch.measurement

import jisa.devices.interfaces.TMeter
import jisa.results.Col
import jisa.results.ResultTable

class TempMeasurement : FetChMeasurement("Temperature Measurement", "Temp", "Temp Measurement") {

    private val pctMargin by userInput("Temperature Stabilization", "Percentage range for temperature to stay within",0.3, )
    private val duration by userInput("Temperature Stabilization", "Duration of temperature stabilization [s]",60.0, ) map { (it * 1e3).toLong() }

    private val tMeter by requiredInstrument("Thermometer", TMeter::class)

    companion object{
        val TEMPERATURE = Col("Temperature", "K")
    }

    override fun getColumns(): Array<Col> {


        return arrayOf(
            TEMPERATURE,
        )
    }
    override fun run(results: ResultTable) {
        tMeter.waitForStableTemperature(pctMargin,duration)
        results.addData(tMeter.temperature)

    }

    override fun onFinish() {
    }
}

