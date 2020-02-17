package org.oefet.fetch.gui.inputs

import jisa.Util
import jisa.devices.SMU
import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.elements.FetChQueue
import org.oefet.fetch.gui.tabs.Configuration

object Stress : Grid("Stress", 2)  {

    private val basic  = Fields("Stress Parameters")
    private val name   = basic.addTextField("Name", "S")

    init { basic.addSeparator() }

    private val drain = basic.addCheckBox("Enable SD", false)
    private val sdV   = basic.addDoubleField("SD Voltage [V]", 50.0)
    private val gate  = basic.addCheckBox("Enabled SG", false)
    private val sgV   = basic.addDoubleField("SG Voltage [V]", 50.0)

    init { basic.addSeparator() }

    private val num   = basic.addIntegerField("No. Intervals", 12)

    private val interval = Fields("Stress Interval")

    private val hrs  = interval.addIntegerField("Hours", 1)
    private val mins = interval.addIntegerField("Minutes", 0)
    private val secs = interval.addIntegerField("Seconds", 0)
    private val mscs = interval.addIntegerField("Milliseconds", 0)

    private val subQueue = ActionQueue()
    private val list     = FetChQueue("Interval Actions", subQueue)

    init {

        addAll(Grid(1, basic, interval), list)
        setIcon(Icon.DIODE)

        list.addTSweep.isDisabled  = true
        list.addTChange.isDisabled = true
        list.addStress.isDisabled  = true

        basic.linkConfig(Settings.stressBasic)
        interval.linkConfig(Settings.stressInterval)

        drain.setOnChange { sdV.isDisabled = !drain.get() }
        gate.setOnChange  { sgV.isDisabled = !gate.get() }

        sdV.isDisabled = !drain.get()
        sgV.isDisabled = !gate.get()

    }

    fun ask(queue: ActionQueue) {

        var i = 0
        while (queue.getVariableCount("S${if (i > 0) i.toString() else ""}") > 0) i++
        name.set("S${if (i > 0) i.toString() else ""}")

        subQueue.clear()

        if (showAndWait()) {


            val name   = name.get()
            val time   = (mscs.get() + (1000 * secs.get()) + (1000 * 60 * mins.get()) + (1000 * 60 * 60 * hrs.get())).toLong()
            val sdHold = drain.get()
            val sgHold = gate.get()
            val sdV    = sdV.get()
            val sgV    = sgV.get()

            repeat(num.get()) {

                queue.addAction((if (sdHold) "SD = $sdV V " else "") + (if (sgHold) "SG = $sgV V " else "") + "for ${Util.msToString(time)}") {

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

                        Thread.sleep(time)

                    } finally {

                        sdSMU?.turnOff()
                        sgSMU?.turnOff()

                    }

                }

                for (action in subQueue) {

                    val copy = action.copy()
                    copy.setVariable(name, Util.msToString((it + 1) * time))
                    if (copy is ActionQueue.MeasureAction) copy.setAttribute(name, "${(it + 1) * time} ms")

                    queue.addAction(copy)

                }

            }

        }

    }

}