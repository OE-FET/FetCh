package org.oefet.fetch

import jisa.Util
import jisa.control.Connection
import jisa.gui.GUI
import jisa.logging.Logger
import org.oefet.fetch.gui.MainWindow
import org.oefet.fetch.gui.Splash
import org.oefet.fetch.gui.tabs.Connections
import org.oefet.fetch.logging.Log
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun main() {

    GUI.touch()

    Connection.addListener { Log.populateSources() }

    Logger.start(
        Util.joinPath(
            System.getProperty("user.home"), "FetCh Logs", "log-${
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(
                    DateTimeFormatter.ISO_DATE_TIME
                ).replace(":","-")
            }.txt"
        )
    )

    Logger.addMessage("FetCh started, initialising...")

    val sysOut = System.out
    val filOut = FileOutputStream(Util.joinPath(System.getProperty("user.home"), "FetChLog.txt"))

    val stream = object : OutputStream() {

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

    Logger.addMessage("Connecting to configured instruments started.")

    val display = Connections.connectAllWithList()
    val numFailed = Connections.connections.count { it.status == Connection.Status.ERROR }

    when {

        Connections.connectors.isEmpty() -> {
            Logger.addMessage("Connecting to configured instruments complete, nothing to connect to.")
            GUI.warningAlert("You have no instruments configured.")
            MainWindow.select(Connections)
        }

        numFailed > 0                    -> {
            Logger.addMessage("Connecting to configured instruments complete, $numFailed failed to connect.")
            GUI.errorAlert("%d %s failed to connect!".format(numFailed, Util.pluralise("instrument", numFailed)))
            MainWindow.select(Connections)
        }

        else                             -> {
            Logger.addMessage("Connecting to configured instruments complete.")
            Util.sleep(1000)
        }

    }

    display.close()

    MainWindow.show()

}