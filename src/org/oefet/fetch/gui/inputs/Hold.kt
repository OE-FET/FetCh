package org.oefet.fetch.gui.inputs

import jisa.Util
import jisa.devices.SMU
import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.MainWindow
import org.oefet.fetch.gui.tabs.Configuration

class Hold : Grid("Voltage Hold", 1), ActionInput {

    private val basic = Fields("Basic Parameters")
    private val time  = basic.addDoubleField("Hold Time [s]", 60.0)

    private val sdConf = Fields("Source-Drain")
    private val sdHold = sdConf.addCheckBox("Enabled", false)
    private val sdV    = sdConf.addDoubleField("Voltage [V]", 50.0)

    private val sgConf = Fields("Source-Gate")
    private val sgHold = sgConf.addCheckBox("Enabled", false)
    private val sgV    = sgConf.addDoubleField("Voltage [V]", 50.0)

    init {

        addAll(basic, Grid(2, sdConf, sgConf))
        basic.linkConfig(Settings.holdBasic)
        sdConf.linkConfig(Settings.holdSD)
        sgConf.linkConfig(Settings.holdSG)

        setIcon(MainWindow::class.java.getResource("fEt.png"))

    }

    override fun ask(queue: ActionQueue) {

        if (showAsConfirmation()) {

            basic.writeToConfig()
            sdConf.writeToConfig()
            sgConf.writeToConfig()

            val time   = time.get()
            val sdHold = sdHold.get()
            val sgHold = sgHold.get()
            val sdV    = sdV.get()
            val sgV    = sgV.get()

            queue.addAction((if (sdHold) "SD = $sdV V " else "") + (if (sgHold) "SG = $sgV V " else "") + "for ${Util.msToString((time * 1000.0).toLong())}") {

                var sdSMU : SMU? = null
                var sgSMU : SMU? = null

                try {

                    if (sdHold) {

                        sdSMU = Configuration.sourceDrain.get() ?: throw Exception("No source-drain channel configured")

                        sdSMU.voltage = sdV
                        sdSMU.turnOn()

                    }

                    if (sgHold) {

                        sgSMU = Configuration.sourceGate.get() ?: throw Exception("No source-gate channel configured")

                        sgSMU.voltage = sgV
                        sgSMU.turnOn()

                    }

                    Thread.sleep((1000.0 * time).toLong())

                } finally {

                    sdSMU?.turnOff()
                    sgSMU?.turnOff()

                }

            }

        }

    }

}