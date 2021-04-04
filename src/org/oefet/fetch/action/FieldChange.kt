package org.oefet.fetch.action

import javafx.scene.paint.Color
import jisa.control.RTask
import jisa.devices.interfaces.EMController
import jisa.experiment.Col
import jisa.experiment.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot

class FieldChange : Action("Change Field") {

    var task: RTask? = null

    val field    by input("Field", "Set-Point [T]", 1.0)
    val fControl by requiredConfig("EM Controller", EMController::class)

    override fun createPlot(data: ResultTable): FetChPlot {

        return FetChPlot("Change Temperature to $field K", "Time [s]", "Temperature [K]").apply {
            createSeries().watch(data, 0, 1).setMarkerVisible(false).setColour(Color.PURPLE)
            isLegendVisible = false
        }

    }

    override fun run(results: ResultTable) {

        task = RTask(2500) { t -> results.addData(t.secFromStart, fControl.field) }
        task?.start()

        fControl.field = field

    }

    override fun onFinish() {
        task?.stop()
    }

    override fun getColumns(): Array<Col> {

        return arrayOf(
            Col("Time","s"),
            Col("Field", "T")
        )

    }

    override fun getLabel(): String {
        return "$field T"
    }

}