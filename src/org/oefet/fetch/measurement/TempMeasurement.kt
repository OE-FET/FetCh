package org.oefet.fetch.measurement

import jisa.devices.interfaces.TMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable

class TempMeasurement : FetChMeasurement("Temperature Measurement", "Temp", "Temp Measurement"){

    private val pctMargin by input("Temperature Stabilization", "Percentage range for temperature to stay within",0.3, )
    private val duration by input("Temperature Stabilization", "Duration of temperature stabilization [s]",60.0, ) map { (it * 1e3).toLong() }

    private val tMeter by requiredConfig("Thermometer", TMeter::class)

    companion object{
        val TEMPERATURE = Col("Temperature", "K")
    }

    override fun getColumns(): Array<Col> {
        tMeter.waitForStableTemperature(pctMargin,duration)
        results.addData(tMeter.temperature)


        return arrayOf(
            TEMPERATURE,
        )
    }
    override fun run(results: ResultTable) {

    }

    override fun onFinish() {
    }
}

