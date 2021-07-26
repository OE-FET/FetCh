package org.oefet.fetch.gui.elements

import jisa.gui.Series.Dash.DOTTED
import jisa.results.ResultTable
import org.oefet.fetch.measurement.Conductivity
import org.oefet.fetch.measurement.TVHighThroughput

class TVHTPlot(data: ResultTable) : FetChPlot("Thermal Voltage", "Temperature Difference [K]", "Voltage [V]") {

    val VOLTAGE  = data.findColumn(TVHighThroughput.VOLTAGE)
    val TEMPERATURE_DIFFERENCE = data.findColumn(TVHighThroughput.TEMPERATURE_DIFFERENCE)
    
    init {

        isMouseEnabled = true
        pointOrdering  = Sort.ORDER_ADDED


        createSeries()
            .setName("FPP Difference")
            .polyFit(1)
            .watch(data, TEMPERATURE_DIFFERENCE, VOLTAGE)


    }

}