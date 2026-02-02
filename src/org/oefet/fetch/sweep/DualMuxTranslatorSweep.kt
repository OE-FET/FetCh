package org.oefet.fetch.sweep

import jisa.Util
import jisa.devices.camera.Camera
import jisa.devices.mux.Multiplexer
import jisa.devices.translator.Translator
import jisa.enums.Icon
import jisa.experiment.queue.Action
import jisa.experiment.queue.SimpleAction
import jisa.gui.Colour
import jisa.gui.Form
import jisa.gui.ImageDisplay
import jisa.maths.Range
import jisa.results.ResultTable
import org.oefet.fetch.FetChEntityAction
import org.oefet.fetch.action.FetChAction
import org.oefet.fetch.quant.Type
import org.oefet.fetch.quant.XYPoint
import org.oefet.fetch.quant.XYZPoint

class DualMuxTranslatorSweep : FetChSweep<MuxPosPair>("Dual Multiplexer and Translator Sweep", "MUX", Type.INDEX, Icon.DEVICE.blackImage) {

    val A      by requiredInstrument("Multiplexer Channel A", Multiplexer::class)
    val B      by requiredInstrument("Multiplexer Channel B", Multiplexer::class)
    val xAxis  by requiredInstrument("X Axis", Translator::class)
    val yAxis  by requiredInstrument("Y Axis", Translator::class)
    val zAxis  by optionalInstrument("Z Axis", Translator::class)
    val camera by optionalInstrument("Camera", Camera::class)

    val routesA by userInput("MUX Channel A", "Routes", Range.linear(0, 23))
    val routesB by userInput("MUX Channel B", "Routes", Range.linear(0, 23))

    val topLeft = Form("Top Left")
    val tlX     = topLeft.addDoubleField("Top-Left X [m]", 0.0)
    val tlY     = topLeft.addDoubleField("Top-Left Y [m]", 0.0)
    val tlZ     = topLeft.addDoubleField("Top-Left Z [m]", 0.0)
    val btn1    = topLeft.addButton("Use Camera...") {

        val point = getPosition()

        if (point != null) {
            topLeftX = point.x
            topLeftY = point.y
            topLeftZ = point.z
        }

    }

    val topRight = Form("Top Right")
    val trX      = topRight.addDoubleField("Top-Right X [m]", 0.0)
    val trY      = topRight.addDoubleField("Top-Right Y [m]", 0.0)
    val trZ      = topRight.addDoubleField("Top-Right Z [m]", 0.0)
    val btn2     = topRight.addButton("Use Camera...") {

        val point = getPosition()

        if (point != null) {
            topRightX = point.x
            topRightY = point.y
            topRightZ = point.z
        }

    }

    val bottomLeft = Form("Bottom Left")
    val blX    = bottomLeft.addDoubleField("Bottom-Left X [m]", 0.0)
    val blY    = bottomLeft.addDoubleField("Bottom-Left Y [m]", 0.0)
    val blZ    = bottomLeft.addDoubleField("Bottom-Left Z [m]", 0.0)
    val btn3   = bottomLeft.addButton("Use Camera...") {

        val point = getPosition()

        if (point != null) {
            bottomLeftX = point.x
            bottomLeftY = point.y
            bottomLeftZ = point.z
        }

    }

    var topLeftX    by customInput(topLeft, tlX)
    var topLeftY    by customInput(topLeft, tlY)
    var topLeftZ    by customInput(topLeft, tlZ)
    var topRightX   by customInput(topRight, trX)
    var topRightY   by customInput(topRight, trY)
    var topRightZ   by customInput(topRight, trZ)
    var bottomLeftX by customInput(bottomLeft, blX)
    var bottomLeftY by customInput(bottomLeft, blY)
    var bottomLeftZ by customInput(bottomLeft, blZ)
    var countX      by userInput("Count", "X", 24)
    var countY      by userInput("Count", "Y", 24)

    val moveDisplay  = ImageDisplay("Moving...")
    val alignDisplay = ImageDisplay("Camera View")

    fun getPosition(): XYZPoint? {

        loadInstruments()

        val camera = this.camera

        if (camera != null) {

            val listener = camera.sendFramesTo(alignDisplay)
            val running  = camera.isAcquiring

            alignDisplay.addCrosshairs(5, Colour.BLACK)
            alignDisplay.addCrosshairs(3, Colour.WHITE)

            if (!running) {
                camera.startAcquisition()
            }

            val result = alignDisplay.showAsConfirmation()

            if (!running) {
                camera.stopAcquisition()
            }

            camera.removeFrameListener(listener)

            if (result) {
                return XYZPoint(xAxis.position, yAxis.position, zAxis?.position ?: 0.0)
            }

        }

        return null

    }

    override fun getValues(): List<MuxPosPair> {

        val list = mutableListOf<MuxPosPair>()

        val rightX = (topRightX - topLeftX) / (countX - 1)
        val rightY = (topRightY - topLeftY) / (countX - 1)
        val rightZ = (topRightZ - topLeftZ) / (countX - 1)
        val downX  = (bottomLeftX - topLeftX) / (countY - 1)
        val downY  = (bottomLeftY - topLeftY) / (countY - 1)
        val downZ  = (bottomLeftZ - topLeftZ) / (countY - 1)

        for (i in 0 until countX) {

            for (j in 0 until countY) {

                val x = topLeftX + (i * rightX) + (j * downX)
                val y = topLeftY + (i * rightY) + (j * downY)
                val z = topLeftZ + (i * rightZ) + (j * downZ)

                list += MuxPosPair(XYPoint(routesA[i], routesB[j]), XYZPoint(x, y, z))

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

        val move = FetChEntityAction(Translate(value.pos.x, value.pos.y, value.pos.z))

        return listOf(switch, move) + actions

    }

    inner class Translate(val x: Double, val y: Double, val z: Double) : FetChAction("Move to (%.02e m, %.02e m)".format(x, y), Icon.COGS.blackImage) {

        override fun createDisplay(data: ResultTable) = moveDisplay.apply {
            addCrosshairs(5, Colour.BLACK)
            addCrosshairs(3, Colour.WHITE)
        }

        override fun run(results: ResultTable?) {

            val listener = camera?.addFrameListener(moveDisplay::drawFrame)
            camera?.startAcquisition()

            xAxis.setPositionAndWait(x)
            yAxis.setPositionAndWait(y)
            zAxis?.setPositionAndWait(z)

            Util.runInParallel(
                { xAxis.waitUntilStationary() },
                { yAxis.waitUntilStationary() },
                { zAxis?.waitUntilStationary() }
            )

            camera?.stopAcquisition()
            camera?.removeFrameListener(listener)

        }

        override fun getLabel(): String {
            return "$x, $y"
        }

    }

}

class MuxPosPair(val mux: XYPoint, val pos: XYZPoint) : XYPoint(mux.x, mux.y)