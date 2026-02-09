package org.oefet.fetch.gui.elements

import javafx.scene.control.Button
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import jisa.devices.camera.Camera
import jisa.devices.translator.Translator
import jisa.gui.ImageDisplay
import jisa.gui.JFXElement

class CameraAligner(val camera: Camera<*>, val x: Translator, val y: Translator) : JFXElement("Camera Aligner") {

    val display = ImageDisplay("Camera View")
    val hbox    = HBox()
    val grid    = GridPane()
    val up      = Button("▲")
    val down    = Button("▼")
    val left    = Button("◀")
    val right   = Button("▶")

    init {

        grid.add(up, 0, 1)
        grid.add(left, 1, 0)
        grid.add(right, 1, 2)
        grid.add(down, 2, 1)

    }

}