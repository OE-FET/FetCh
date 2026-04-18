package org.oefet.fetch.gui

import javafx.scene.image.Image
import jisa.gui.SplashScreen
import org.oefet.fetch.gui.images.Images

object Splash : SplashScreen("FetCh", Image(Images.getURL("splash.png").toExternalForm(), 660.0, 382.0, true, true)) {

    init {
        setIcon(Images.getURL("fetch.svg.png"))
        setWindowSize(600.0, 382.0)
    }

}