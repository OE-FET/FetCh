package org.oefet.fetch.sweep

import jisa.Util
import jisa.devices.camera.Camera
import jisa.devices.translator.Translator
import jisa.enums.Icon
import jisa.experiment.queue.Action
import jisa.gui.ImageDisplay
import jisa.maths.Range
import jisa.results.ResultTable
import org.oefet.fetch.FetChEntityAction
import org.oefet.fetch.action.FetChAction
import org.oefet.fetch.quant.Type
import org.oefet.fetch.quant.XYZPoint

class TranslatorSweep : FetChSweep<XYZPoint>("Translator Sweep", "POS", Type.DISTANCE, Icon.COGS.blackImage) {

    val x by userInput("X Values [m]", Range.linear(-10e-3, 10e-3, 11))
    val y by userInput("Y Values [m]", Range.linear(-10e-3, 10e-3, 11))
    val z by userInput("Z Values [m]", Range.linear(-10e-3, 10e-3, 11))

    val xAxis by optionalInstrument("X Axis", Translator::class)
    val yAxis by optionalInstrument("Y Axis", Translator::class)
    val zAxis by optionalInstrument("Z Axis", Translator::class)
    val camera by optionalInstrument("Camera", Camera::class)

    override fun getValues(): List<XYZPoint> {

        val list = ArrayList<XYZPoint>()

        for (zv in z) {

            for (yv in y) {

                for (xv in x) {

                    list += XYZPoint(xv, yv, zv)

                }

            }

        }

        return list

    }

    val disp = ImageDisplay("Moving...").apply {

        maxWidth = 500.0
        maxHeight = 500.0

    }

    inner class Translate(val x: Double, val y: Double, val z: Double) : FetChAction("Translate", Icon.COGS.blackImage) {

        override fun createDisplay(data: ResultTable) = disp

        override fun run(results: ResultTable?) {

            val listener = camera?.addFrameListener(disp::drawFrame)
            camera?.startAcquisition()

            xAxis?.setPositionAndWait(x)
            yAxis?.setPositionAndWait(y)
            zAxis?.setPositionAndWait(z)

            Util.runInParallel(
                { xAxis?.waitUntilStationary() },
                { yAxis?.waitUntilStationary() },
                { zAxis?.waitUntilStationary() }
            )

            camera?.stopAcquisition()
            camera?.removeFrameListener(listener)

        }

        override fun getLabel(): String {
            return "$x, $y, $z"
        }

    }

    override fun generateForValue(value: XYZPoint, actions: List<Action<*>>): List<Action<*>> {

        val list = ArrayList<Action<*>>()

        list += FetChEntityAction(Translate(value.x, value.y, value.z))
        list += actions

        return list

    }

    override fun formatValue(value: XYZPoint): String {
        return "${value.x}, ${value.y}, ${value.z}"
    }


}