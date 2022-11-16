package org.oefet.fetch.measurement

import jisa.devices.interfaces.VMeter
import jisa.enums.Icon
import jisa.maths.Range
import jisa.results.Column
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot

class VMeasure : FetChMeasurement("Voltage Measurement", "Voltage", "VMeasure", Icon.VOLTMETER.blackImage) {

    val totalTime    by userTimeInput("Timing", "Total Time", 10000)
    val numIntervals by userInput("Timing", "No. Measurements", 10)
    val vMeter       by requiredInstrument("Voltmeter", VMeter::class)

    companion object Columns {

        val TIME    = Column.forDecimal("Time", "s")
        val VOLTAGE = Column.forDecimal("Voltage", "V")
        val COLUMNS = arrayOf(TIME, VOLTAGE)

    }

    override fun createDisplay(data: ResultTable): FetChPlot {

        val plot = FetChPlot("Voltage Measurement")

        plot.createSeries().watch(data, TIME, VOLTAGE)

        return plot

    }

    override fun run(results: ResultTable) {

        vMeter.turnOn()

        val interval = totalTime / numIntervals

        for (time in Range.linear(0, totalTime, numIntervals)) {

            results.mapRow(
                TIME    to time / 1e3,
                VOLTAGE to vMeter.voltage
            )

            sleep(interval)

        }


    }

    override fun onFinish() {
        vMeter.turnOff()
    }

    override fun getColumns() = COLUMNS

}