package fetter.gui

import fetter.analysis.FETMeasurement
import fetter.analysis.OCurve
import fetter.analysis.TCurve
import fetter.measurement.OutputMeasurement
import fetter.measurement.TransferMeasurement
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


        val grid = Grid(2)

        if (Measure.makeTables.get()) {
            val table = Table("Data", action.data)
            grid.add(table)
        }

        if (Measure.makePlots.get()) {

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

        }

        add(Section(action.name, grid))

    }

}