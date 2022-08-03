package org.oefet.fetch.gui.elements

import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import jisa.gui.JFXElement
import org.oefet.fetch.gui.fxml.FXMLFiles
import org.oefet.fetch.gui.images.Images

class Wizard : JFXElement("FetCh Setup", FXMLFiles::class.java.getResource("WelcomeWizard.fxml")) {

    lateinit var root: BorderPane
    lateinit var image: ImageView

    init {
        image.image = Images.getImage("fEt.png")
        image.fitHeight = 128.0
    }

}