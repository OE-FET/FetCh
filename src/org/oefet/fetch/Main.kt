package org.oefet.fetch

import org.oefet.fetch.gui.MainWindow
import jisa.gui.Doc
import jisa.gui.GUI
import org.oefet.fetch.gui.Splash

class Main;

fun main() {

    Splash.show()
    MainWindow.show()
    Splash.close()

    println(MainWindow.node.stylesheets)

}