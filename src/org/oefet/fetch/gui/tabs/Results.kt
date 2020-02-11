package org.oefet.fetch.gui.tabs

import org.oefet.fetch.analysis.MeasurementFile
import org.oefet.fetch.analysis.OCurve
import org.oefet.fetch.analysis.TCurve
import org.oefet.fetch.measurement.OutputMeasurement
import org.oefet.fetch.measurement.TransferMeasurement
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Grid
import jisa.gui.Plot
import jisa.gui.Section
import jisa.gui.Table
import org.oefet.fetch.gui.elements.FPPPlot
import org.oefet.fetch.gui.elements.OutputPlot
import org.oefet.fetch.gui.elements.TransferPlot
import org.oefet.fetch.measurement.FPPMeasurement

object Results : Grid("Results", 1) {

    init {
        setGrowth(true, false)
        setIcon(Icon.DATA)
    }

    fun add(action: ActionQueue.MeasureAction) {


        val grid = Grid(2)
        val table = Table("Data", action.data)
        grid.add(table)

        val plot = when (action.measurement) {

            is OutputMeasurement -> OutputPlot(OCurve(action.data))

            is TransferMeasurement -> TransferPlot(TCurve(action.data))

            is FPPMeasurement -> FPPPlot(action.data)

            else -> Plot("Unknown")

        }

        grid.add(plot)
        add(Section(action.name, grid).apply { isExpanded = false })

    }

}