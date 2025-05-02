package org.oefet.fetch.measurement

import javafx.scene.paint.Color
import jisa.devices.meter.IMeter
import jisa.devices.meter.VMeter
import jisa.enums.Icon
import jisa.gui.Plot
import jisa.maths.Range
import jisa.results.Column
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot

class IVMonitor : FetChMeasurement("IV Monitor", "IVMonitor", Icon.VOLTMETER.blackImage) {

    val intervalTime  by userTimeInput("Measurement", "Interval", 1000)
    val totalTime     by userTimeInput("Measurement", "Total Time", 100000)
    val autoOffV      by userInput("Auto Turn Off", "Voltmeter", true)
    val autoOffI      by userInput("Auto Turn Off", "Ammeter", true)
    val vMeter        by optionalInstrument("Voltmeter", VMeter::class)
    val iMeter        by optionalInstrument("Ammeter", IMeter::class)

    companion object {

        val TIME    = Column.ofLongs("Time", "ms")
        val VOLTAGE = Column.ofDoubles("Voltage", "V")
        val CURRENT = Column.ofDoubles("Current", "A")

    }

    override fun createDisplay(data: ResultTable): FetChPlot {

        return FetChPlot("IV Monitor").apply {

            xLabel = "Time"

            if (data.any { it[VOLTAGE].isFinite() } || vMeter != null) {
                createSeries().watch(data, { it[TIME] / 1e3 }, { it[VOLTAGE] }).setColour(Color.RED)
                yLabel = "Voltage [V]"
            } else if (data.any { it[CURRENT].isFinite() } || iMeter != null) {
                createSeries().watch(data, { it[TIME] / 1e3 }, { it[CURRENT] }).setColour(Color.CORNFLOWERBLUE)
                yLabel = "Current [A]"
            }

            isLegendVisible = false
            xAxisType       = Plot.AxisType.TIME

        }

    }

    override fun getColumns(): Array<Column<*>> = arrayOf(TIME, VOLTAGE, CURRENT)

    override fun run(results: ResultTable) {

        vMeter?.turnOn()
        iMeter?.turnOn()

        for (time in Range.step(0, totalTime, intervalTime)) {

            results.mapRow(
                TIME    to System.currentTimeMillis(),
                VOLTAGE to (vMeter?.voltage ?: Double.NaN),
                CURRENT to (iMeter?.current ?: Double.NaN)
            )

            if (time < totalTime) {
                sleep(intervalTime)
            }

        }

    }

    override fun onFinish() {

        if (autoOffV) {
            vMeter?.turnOff()
        }

        if (autoOffI) {
            iMeter?.turnOff()
        }

    }


}