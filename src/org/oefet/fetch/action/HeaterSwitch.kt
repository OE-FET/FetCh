package org.oefet.fetch.action

import jisa.control.RTask
import jisa.devices.interfaces.EMController
import jisa.devices.power.IPS120
import jisa.enums.Icon
import jisa.gui.Colour
import jisa.results.ResultTable
import org.oefet.fetch.gui.elements.FetChPlot

class HeaterSwitch : FetChAction("Heater Switch", Icon.RESISTOR.blackImage) {

    var task: RTask? = null

    val on  by userInput ("Basic", "On", false)
    val ips by requiredInstrument("IPS", EMController::class)

    val plot = FetChPlot("Heater Switch").apply { isLegendVisible = false; }

    override fun createDisplay(data: ResultTable): FetChPlot = plot

    override fun run(results: ResultTable) {

        val ips = ips

        if (ips !is IPS120) {
            throw Exception("This is not an IPS120")
        }

        plot.clear()

        val background = plot.createSeries().setLineWidth(30.0).setColour(Colour.GREY).setMarkerVisible(false)
        val fill       = plot.createSeries().setLineWidth(20.0).setColour(Colour.BLUE).setMarkerVisible(false)

        background.addPoint(0.0, 0.0).addPoint(30.0, 0.0)

        task = RTask((300).toLong()) { t -> fill.addPoint(t.secFromStart, 0.0) }

        task?.start()

        ips.setHeater(on)

    }

    override fun onFinish() {
        task?.stop()
    }

    override fun getLabel(): String {
        return if (on) "On" else "Off"
    }

}