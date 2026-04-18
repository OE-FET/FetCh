package org.oefet.fetch.sweep

import jisa.Util
import jisa.devices.pid.TC
import jisa.enums.Icon
import jisa.experiment.queue.Action
import jisa.experiment.queue.SimpleAction
import jisa.maths.Range
import org.oefet.fetch.gui.images.Images

class DoubleTSweep: FetChSweep<Pair<Double, Double>>("Double Temperature Sweep", "DT", Icon.THERMOMETER.blackImage) {

    val temperatures1 by userInput("Temperatures", "List 1 [K]", Range.linear(100, 300, 3))
    val temperatures2 by userInput("Temperatures", "List 2 [K]", Range.linear(300, 300, 3))
    val stabilityPct  by userInput("Temperature", "Stays within [%]", 1.0)
    val stabilityTime by userTimeInput("Temperature", "For at least", 600000)
    val autoOff1 by userInput("Temperature", "Auto-Off Controller 1", false)
    val autoOff2 by userInput("Temperature", "Auto-Off Controller 2", false)

    val tc1 by requiredInstrument("Controller 1", TC.Loop::class)
    val tc2 by requiredInstrument("Controller 2", TC.Loop::class)

    override fun getValues(): List<Pair<Double, Double>> {
        return temperatures1.zip(temperatures2).toList()
    }

    override fun formatValue(value: Pair<Double, Double>): String {
        return "(T1 = ${value.first} K, T2 = ${value.second} K)"
    }

    override fun generateForValue(value: Pair<Double, Double>, actions: List<Action<*>>): List<Action<*>> {

        val action = SimpleAction("Set Temperatures ${formatValue(value)}") {

            tc1.setPoint = value.first
            tc2.setPoint = value.second

            tc1.isPIDEnabled = true
            tc2.isPIDEnabled = true

            Util.runInParallel({
                tc1.waitForStableTemperature(value.first, stabilityPct, stabilityTime.toLong())
            }, {
                tc2.waitForStableTemperature(value.second, stabilityPct, stabilityTime.toLong())
            })

        }

        return listOf(action) + actions

    }

    override fun onFinish() {

        if (autoOff1) {
            runRegardless(
                { tc1.manualValue = 0.0 },
                { tc1.isPIDEnabled = false }
            )
        }
        if (autoOff2) {
            runRegardless(
                { tc2.manualValue = 0.0 },
                { tc2.isPIDEnabled = false }
            )
        }
    }

}