package org.oefet.fetch.gui

import org.oefet.fetch.analysis.FETMeasurement
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

object Results : Grid("Results", 1) {

    init {
        setGrowth(true, false)
        setIcon(Icon.DATA)
    }

    fun addMeasurement(action: ActionQueue.MeasureAction) {


        val grid  = Grid(2)
        val table = Table("Data", action.data)
        grid.add(table)

        val plot = when (action.measurement) {

            is OutputMeasurement -> OutputPlot(
                OCurve(
                    Measure.length.get(),
                    Measure.width.get(),
                    FETMeasurement.EPSILON * Measure.dielConst.get() / Measure.thick.get(),
                    action.data
                )
            )
            is TransferMeasurement -> TransferPlot(
                TCurve(
                    Measure.length.get(),
                    Measure.width.get(),
                    FETMeasurement.EPSILON * Measure.dielConst.get() / Measure.thick.get(),
                    action.data
                )
            )
            else -> Plot("Unknown")

        }

        grid.add(plot)
        add(Section(action.name, grid).apply { isExpanded = false })

    }

}