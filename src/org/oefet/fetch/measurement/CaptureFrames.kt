package org.oefet.fetch.measurement

import jisa.devices.camera.Camera
import jisa.devices.camera.frame.Frame
import jisa.gui.ImageDisplay
import jisa.results.ResultTable
import java.util.*

class CaptureFrames : FetChMeasurement("Capture Frames") {

    val count    by userInput("Basic", "Count", 1)
    val interval by userTimeInput("Basic", "Interval", 500)
    val camera   by requiredInstrument("Camera", Camera::class)
    var path = ""

    val display = ImageDisplay("Capturing Frames...")

    override fun newResults(path: String): ResultTable {
        this.path = path
        return newResults()
    }

    companion object : Columns() {
        val FRAME = integerColumn("Frame")
        val X     = integerColumn("X")
        val Y     = integerColumn("Y")
        val ARGB  = integerColumn("Value")
    }

    override fun run(results: ResultTable) {

        val frames = LinkedList<Frame<*,*>>()

        for (i in 0 until count) {

            val frame = camera.frame
            frames += frame

            display.drawFrame(frame)

            frame.savePNG(path.replace(".csv", "-$i.png"))

            sleep(interval)

        }

    }

    override fun createDisplay(data: ResultTable) = display

    override fun onFinish() {

    }
}