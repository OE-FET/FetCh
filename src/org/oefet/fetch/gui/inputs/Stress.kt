package org.oefet.fetch.gui.inputs

import jisa.Util
import jisa.devices.interfaces.SMU
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Configurator
import jisa.gui.Fields
import jisa.gui.Grid
import jisa.gui.Tabs
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.elements.FetChQueue
import org.oefet.fetch.gui.images.Images

class Stress : Tabs("Stress"), SweepInput {

    private val sd = Configurator<SMU>("Source-Drain Channel", SMU::class.java)
    private val sg = Configurator<SMU>("Source-Gate Channel", SMU::class.java)

    private val params      = Grid("Parameters", 2)
    private val instruments = Grid("Instruments", 2, sd, sg)
    private val basic       = Fields("Stress Parameters")
    private val name        = basic.addTextField("Variable Name", "S").apply { isDisabled = true }

    init {
        basic.addSeparator()
    }

    private val drain = basic.addCheckBox("Enable SD", false)
    private val sdV   = basic.addDoubleField("SD Voltage [V]", 50.0)
    private val gate  = basic.addCheckBox("Enabled SG", false)
    private val sgV   = basic.addDoubleField("SG Voltage [V]", 50.0)

    init {
        basic.addSeparator()
    }

    private val num = basic.addIntegerField("No. Intervals", 12)

    private val interval = Fields("Stress Interval")

    private val hrs = interval.addIntegerField("Hours", 1)
    private val mins = interval.addIntegerField("Minutes", 0)
    private val secs = interval.addIntegerField("Seconds", 0)
    private val mscs = interval.addIntegerField("Milliseconds", 0)

    private val subQueue = ActionQueue()

    init {


        params.setGrowth(true, false)
        instruments.setGrowth(true, false)

        setIcon(Icon.DIODE)

        drain.setOnChange { sdV.isDisabled = !drain.get() }
        gate.setOnChange { sgV.isDisabled = !gate.get() }

        sdV.isDisabled = !drain.get()
        sgV.isDisabled = !gate.get()

        setIcon(Images.getURL("fEt.png"))

        addAll(params, instruments)

    }

    override fun ask(queue: ActionQueue) {

        basic.loadFromConfig(Settings.stressBasic)
        interval.loadFromConfig(Settings.stressInterval)
        sd.loadFromConfig(Settings.holdSDConf)
        sg.loadFromConfig(Settings.holdSGConf)

        params.clear()
        params.addAll(Grid(1, basic, interval), FetChQueue("Interval Actions", subQueue))

        subQueue.clear()

        if (showAsConfirmation()) {

            basic.writeToConfig(Settings.stressBasic)
            interval.writeToConfig(Settings.stressInterval)

            sd.writeToConfig(Settings.holdSDConf)
            sg.writeToConfig(Settings.holdSGConf)

            val name = name.get()
            val time =
                (mscs.get() + (1000 * secs.get()) + (1000 * 60 * mins.get()) + (1000 * 60 * 60 * hrs.get())).toLong()
            val sdHold = drain.get()
            val sgHold = gate.get()
            val sdV = sdV.get()
            val sgV = sgV.get()

            repeat(num.get()) {

                queue.addAction(
                    (if (sdHold) "SD = $sdV V " else "") + (if (sgHold) "SG = $sgV V " else "") + "for ${Util.msToString(
                        time
                    )}"
                ) {

                    var sdSMU: SMU? = null
                    var sgSMU: SMU? = null

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

                        Thread.sleep(time)

                    } finally {

                        sdSMU?.turnOff()
                        sgSMU?.turnOff()

                    }

                }

                for (action in subQueue) {

                    val copy = action.copy()
                    copy.setVariable(name, Util.msToString((it + 1) * time))
                    if (copy is ActionQueue.MeasureAction) copy.setAttribute(
                        name,
                        "${((it + 1) * time).toDouble() / 1000.0} s"
                    )

                    queue.addAction(copy)

                }

            }

        }

    }

}