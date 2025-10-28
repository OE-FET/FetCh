package org.oefet.fetch.action

import jisa.Util
import jisa.devices.camera.Camera
import jisa.devices.translator.Translator
import jisa.enums.Icon
import jisa.gui.Doc
import jisa.gui.Element
import jisa.gui.ImageDisplay
import jisa.results.ResultTable

class Translate : FetChAction("Translate", Icon.COGS.blackImage) {

    val xAxis  by optionalInstrument("X Axis", Translator::class)
    val yAxis  by optionalInstrument("Y Axis", Translator::class)
    val zAxis  by optionalInstrument("Z Axis", Translator::class)
    val camera by optionalInstrument("Camera", Camera::class)

    val x by userInput("X [m]", 0.0)
    val y by userInput("Y [m]", 0.0)
    val z by userInput("Z [m]", 0.0)

    val disp = ImageDisplay("Moving...")

    override fun createDisplay(data: ResultTable): Element {

        if (camera != null) {
            return disp
        } else {
            return Doc("Moving...")
        }

    }

    override fun run(results: ResultTable?) {

        val listener = camera?.addFrameListener(disp::drawFrame)
        camera?.startAcquisition()

        xAxis?.position = x
        yAxis?.position = y
        zAxis?.position = z

        Util.runInParallel(
            { xAxis?.waitUntilStationary() },
            { yAxis?.waitUntilStationary() },
            { zAxis?.waitUntilStationary() }
        )

        camera?.stopAcquisition()

    }

}