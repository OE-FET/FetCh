package org.oefet.fetch.gui.tabs

import jisa.enums.Icon
import jisa.gui.ConnectorGrid
import jisa.logging.Logger
import jssc.SerialPort
import jssc.SerialPortList
import org.oefet.fetch.Settings

object Connections : ConnectorGrid("Connections", 3) {


    init {

        numColumns = 3

        setIcon(Icon.CONNECTION)

        linkToConfig(Settings.connections)

        addToolbarButton("Clear All Serial Ports") {

            SerialPortList.getPortNames().map(::SerialPort).forEach {

                Logger.addMessage("Clearing Serial Port \"$it.portName\"...")

                try {
                    it.purgePort(SerialPort.PURGE_RXABORT or SerialPort.PURGE_TXABORT or SerialPort.PURGE_RXCLEAR or SerialPort.PURGE_TXCLEAR)
                    it.closePort()
                } catch (e: Throwable) {
                    Logger.addError(e.message)
                    e.printStackTrace()
                }

            }
        }

    }

}