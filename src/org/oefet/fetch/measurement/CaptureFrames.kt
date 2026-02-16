package org.oefet.fetch.measurement

import jisa.devices.camera.Camera
import jisa.gui.ImageDisplay
import jisa.results.ResultTable
import org.oefet.fetch.data.FetChData

class CaptureFrames : Measurement("Capture Frames") {

    val count    by userInput("Basic", "Count", 1)
    val interval by userTimeInput("Basic", "Interval", 500)
    val camera   by requiredInstrument("Camera", Camera::class)

    val display = ImageDisplay("Capturing Frames...")

    override fun main(results: ResultTable) {

        for (i in 0 until count) {

            val frame = camera.frame

            display.drawFrame(frame)

            frame.savePNG(currentFile.replace(".csv", "-$i.png"))

            sleep(interval)

        }

    }

    override fun after(data: ResultTable?) { }

    override fun createDisplay(data: ResultTable) = display

    override fun processResults(data: ResultTable): FetChData? = null

}