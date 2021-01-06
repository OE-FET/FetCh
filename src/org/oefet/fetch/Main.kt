package org.oefet.fetch

import jisa.Util
import jisa.control.Connection
import jisa.gui.GUI
import org.oefet.fetch.gui.MainWindow
import org.oefet.fetch.gui.Splash
import org.oefet.fetch.gui.tabs.Connections

fun main() {

    Splash.show()
    MainWindow.select(0)
    Splash.close()

    val display   = Connections.connectAllWithList()
    val numFailed = Connections.connections.count { it.status == Connection.Status.ERROR }

    when {

        Connections.connectors.isEmpty() -> {
            GUI.warningAlert("You have no instruments configured.")
            MainWindow.select(Connections)
        }

        numFailed > 0                    -> {
            GUI.errorAlert("%d %s failed to connect!".format(numFailed, Util.pluralise("instrument", numFailed)))
            MainWindow.select(Connections)
        }

        else                             -> Util.sleep(1000)

    }

    display.close()
    MainWindow.show()

}