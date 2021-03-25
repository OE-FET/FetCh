package org.oefet.fetch.gui.inputs

import jisa.Util
import jisa.devices.interfaces.SMU
import jisa.experiment.ActionQueue
import jisa.gui.Configurator
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.images.Images

class Hold : Grid("Voltage Hold", 1), ActionInput {

    private val basic = Fields("Basic Parameters")
    private val time  = basic.addDoubleField("Hold Time [s]", 60.0)

    private val sdConf = Fields("Source-Drain")
    private val sdHold = sdConf.addCheckBox("Enabled", false)
    private val sdV    = sdConf.addDoubleField("Voltage [V]", 50.0)

    private val sgConf = Fields("Source-Gate")
    private val sgHold = sgConf.addCheckBox("Enabled", false)
    private val sgV    = sgConf.addDoubleField("Voltage [V]", 50.0)

    private val sd = Configurator<SMU>("Source-Drain Channel", SMU::class.java)
    private val sg = Configurator<SMU>("Source-Gate Channel", SMU::class.java)

    private var action : ActionQueue.Action? = null



    init {

        addAll(basic, Grid(2, sdConf, sgConf), Grid(2, sd, sg))

        setIcon(Images.getURL("fEt.png"))

    }

    override fun edit() {

        showAsAlert()

        basic.writeToConfig(Settings.holdBasic)
        sdConf.writeToConfig(Settings.holdSD)
        sgConf.writeToConfig(Settings.holdSG)

        action?.name = "Hold " + (if (sdHold.value) "SD = $sdV V " else "") + (if (sgHold.value) "SG = $sgV V " else "") + "for ${Util.msToString((time.value * 1000.0).toLong())}"

    }

    override fun ask(queue: ActionQueue) {

        basic.loadFromConfig(Settings.holdBasic)
        sdConf.loadFromConfig(Settings.holdSD)
        sgConf.loadFromConfig(Settings.holdSG)
        sd.loadFromConfig(Settings.holdSDConf)
        sg.loadFromConfig(Settings.holdSGConf)

        if (showAsConfirmation()) {

            basic.writeToConfig(Settings.holdBasic)
            sdConf.writeToConfig(Settings.holdSD)
            sgConf.writeToConfig(Settings.holdSG)

            sd.writeToConfig(Settings.holdSDConf)
            sg.writeToConfig(Settings.holdSGConf)


            action = queue.addAction("Hold " + (if (sdHold.value) "SD = $sdV V " else "") + (if (sgHold.value) "SG = $sgV V " else "") + "for ${Util.msToString((time.value * 1000.0).toLong())}") {

                val time   = time.value
                val sdHold = sdHold.value
                val sgHold = sgHold.value
                val sdV    = sdV.value
                val sgV    = sgV.value

                var sdSMU : SMU? = null
                var sgSMU : SMU? = null

                try {

                    if (sdHold) {

                        sdSMU = sd.configuration.get() ?: throw Exception("No source-drain channel configured")

                        sdSMU.voltage = sdV
                        sdSMU.turnOn()

                    }

                    if (sgHold) {

                        sgSMU = sg.configuration.get() ?: throw Exception("No source-gate channel configured")

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