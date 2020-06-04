package org.oefet.fetch.gui

import jisa.gui.SplashScreen
import org.oefet.fetch.gui.images.Images

object Splash : SplashScreen("FetCh", Images.getURL("splash.png")) {

    init {
        setIcon(Images.getURL("fEt.png"))
    }

}