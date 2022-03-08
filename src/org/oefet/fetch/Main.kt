package org.oefet.fetch

import jisa.Util
import jisa.control.Connection
import jisa.gui.GUI
import jisa.results.Column
import jisa.results.ResultTable
import org.oefet.fetch.gui.MainWindow
import org.oefet.fetch.gui.Splash
import org.oefet.fetch.gui.tabs.Connections
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream

fun ResultTable.mapRow(vararg data: Pair<Column<*>, Any>) {
    mapRow(mapOf(*data))
}

fun main() {

    val sysOut = System.out
    val filOut = FileOutputStream(Util.joinPath(System.getProperty("user.home"), "FetChLog.txt"))

    val stream = object: OutputStream() {

        override fun write(b: Int) {
            sysOut.write(b)
            filOut.write(b)
        }

    }

    System.setErr(PrintStream(stream))
    System.setOut(PrintStream(stream))

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