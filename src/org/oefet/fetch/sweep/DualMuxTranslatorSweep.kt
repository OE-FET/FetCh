package org.oefet.fetch.sweep

import jisa.Util
import jisa.devices.camera.Camera
import jisa.devices.mux.Multiplexer
import jisa.devices.translator.Translator
import jisa.enums.Icon
import jisa.experiment.queue.Action
import jisa.experiment.queue.SimpleAction
import jisa.gui.ImageDisplay
import jisa.maths.Range
import jisa.results.ResultTable
import org.oefet.fetch.FetChEntityAction
import org.oefet.fetch.action.FetChAction
import org.oefet.fetch.quant.Type
import org.oefet.fetch.quant.XYPoint

class DualMuxTranslatorSweep : FetChSweep<MuxPosPair>("Dual Multiplexer and Translator Sweep", "XY", Type.DISTANCE, Icon.DEVICE.blackImage) {

    val A      by requiredInstrument("Multiplexer Channel A", Multiplexer::class)
    val B      by requiredInstrument("Multiplexer Channel B", Multiplexer::class)
    val xAxis  by requiredInstrument("X Axis", Translator::class)
    val yAxis  by requiredInstrument("Y Axis", Translator::class)
    val camera by optionalInstrument("Camera", Camera::class)

    val routesA    by userInput("MUX Channel A", "Routes", Range.linear(0, 23))
    val routesB    by userInput("MUX Channel B", "Routes", Range.linear(0, 23))
    val xPositions by userInput("X Axis", "Positions [m]", Range.linear(-10e-3, 10e-3, 11))
    val yPositions by userInput("Y Axis", "Positions [m]", Range.linear(-10e-3, 10e-3, 11))

    val disp = ImageDisplay("Moving...")

    override fun getValues(): List<MuxPosPair> {

        val list = mutableListOf<MuxPosPair>()

        for ((a, x) in routesA.zip(xPositions)) {
            for ((b, y) in routesB.zip(yPositions)) {
                list.add(MuxPosPair(XYPoint(a, b), XYPoint(x, y)))
            }
        }

        return list.toList()

    }

    override fun formatValue(value: MuxPosPair): String {
        return "%d, %d".format(value.mux.x.toInt(), value.mux.y.toInt())
    }

    override fun generateForValue(value: MuxPosPair, actions: List<Action<*>>): List<Action<*>> {

        val switch = SimpleAction("Change MUX Route to (%d, %d)".format(value.mux.x.toInt(), value.mux.y.toInt())) {
            A.route        = value.mux.x.toInt()
            B.route        = value.mux.y.toInt()
        }

        val move = FetChEntityAction(Translate(value.pos.x, value.pos.y))

        return listOf(switch, move) + actions

    }

    inner class Translate(val x: Double, val y: Double) : FetChAction("Translate", Icon.COGS.blackImage) {

        override fun createDisplay(data: ResultTable) = disp

        override fun run(results: ResultTable?) {

            val listener = camera?.addFrameListener(disp::drawFrame)
            camera?.startAcquisition()

            xAxis?.setPositionAndWait(x)
            yAxis?.setPositionAndWait(y)

            Util.runInParallel(
                { xAxis?.waitUntilStationary() },
                { yAxis?.waitUntilStationary() },
            )

            camera?.stopAcquisition()
            camera?.removeFrameListener(listener)

        }

        override fun getLabel(): String {
            return "$x, $y"
        }

    }

}

class MuxPosPair(val mux: XYPoint, val pos: XYPoint) : XYPoint(pos.x, pos.y)