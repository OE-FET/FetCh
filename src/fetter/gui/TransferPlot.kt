package fetter.gui

import fetter.analysis.TCurve
import jisa.experiment.ResultTable
import jisa.gui.*
import kotlin.math.abs

class TransferPlot(data: TCurve) : Plot("Transfer Curve", "SG Voltage [V]", "Current [A]") {

    init {

        useMouseCommands(true)
        setYAxisType(AxisType.LOGARITHMIC)
        setPointOrdering(Sort.ORDER_ADDED)

        createSeries()
            .showMarkers(false)
            .watch(data.data, { it[SG_VOLTAGE] }, { abs(it[SD_CURRENT]) })
            .split(SET_SD, "D (SD: %s V)")

        createSeries()
            .showMarkers(false)
            .setLineDash(Series.Dash.DOTTED)
            .watch(data.data, { it[SG_VOLTAGE] }, { abs(it[SG_CURRENT]) })
            .split(SET_SD, "G (SD: %sV)")


        addSaveButton("Save")
        addToolbarSeparator()
        addToolbarButton("Linear") { setYAxisType(AxisType.LINEAR) }
        addToolbarButton("Logarithmic") { setYAxisType(AxisType.LOGARITHMIC) }

        addToolbarSeparator()

        addToolbarButton("Mobility") {

            if (!data.calculated) data.calculate()

            val mobPlot = Plot("Mobility", "SG Voltage [V]", "Mobility [cm^2/Vs]")
            val fwdTable = Table("Forward Sweep", data.fwdMob)
            val bwdTable = Table("Backward Sweep", data.bwdMob)

            mobPlot.createSeries()
                .showMarkers(false)
                .watch(data.fwdMob, TCurve.SG_VOLTAGE, TCurve.MOBILITY)
                .split(TCurve.SD_VOLTAGE, "Fwd SD: %s V")

            mobPlot.createSeries()
                .setLineDash(Series.Dash.DOTTED)
                .showMarkers(false)
                .watch(data.bwdMob, TCurve.SG_VOLTAGE, TCurve.MOBILITY)
                .split(TCurve.SD_VOLTAGE, "Bwd SD: %s V")

            mobPlot.useMouseCommands(true)

            Grid("Mobilities", 1, mobPlot, Grid(2, fwdTable, bwdTable)).apply {

                addToolbarButton("Save Data") {
                    val file = GUI.saveFileSelect()

                    if (file != null) {
                        data.fwdMob.output("$file-transfer-fwd.csv")
                        data.bwdMob.output("$file-transfer-bwd.csv")
                    }

                }
                addToolbarSeparator()
                addToolbarButton("Save Plot") { mobPlot.showSaveDialog() }
                addToolbarSeparator()
                addToolbarButton("Linear Plot") { mobPlot.setYAxisType(AxisType.LINEAR) }
                addToolbarButton("Logarithmic Plot") { mobPlot.setYAxisType(AxisType.LOGARITHMIC) }
            }.show()

        }

    }

}