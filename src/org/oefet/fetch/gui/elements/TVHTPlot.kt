package org.oefet.fetch.gui.elements

import jisa.results.ResultTable
import org.oefet.fetch.measurement.TVHighThroughput

class TVHTPlot(data: ResultTable) : FetChPlot("Thermal Voltage", "Temperature Difference [K]", "Voltage [V]") {

    val VOLTAGE  = data.findColumn(TVHighThroughput.VOLTAGE)
    val TEMPERATURE_DIFFERENCE = data.findColumn(TVHighThroughput.TEMPERATURE_DIFFERENCE)
    val VOLTAGESTDDEVIATION = data.findColumn(TVHighThroughput.VOLTAGE_ERROR)


    init {

        isMouseEnabled = true
        


        createSeries()
            .setName("FPP Difference")
            .polyFit(1)
            .watch(data, TEMPERATURE_DIFFERENCE, VOLTAGE, VOLTAGESTDDEVIATION)



    }

}