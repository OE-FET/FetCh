package org.oefet.fetch.sweep

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.SMU
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementAction
import jisa.gui.Colour
import jisa.maths.Range
import org.oefet.fetch.action.FAction
import org.oefet.fetch.gui.elements.FetChPlot
import java.util.*

class StressSweep : Sweep<Int>("Stress") {

    var task: RTask? = null

    val time by input("Basic", "Interval Time [s]", 600.0) map { it.toMSec() }
    val count by input("Basic", "No. Intervals", 10)
    val interval by input("Basic", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val useSD by input("Source-Drain", "Enabled", false)
    val sdVoltage by input("Source-Drain", "Voltage [V]", 50.0)
    val useSG by input("Source-Gate", "Enabled", false)
    val sgVoltage by input("Source-Gate", "Voltage [V]", 50.0)

    val sdSMU by optionalConfig("Source-Drain Channel", SMU::class) requiredIf { useSD }
    val sgSMU by optionalConfig("Source-Gate Channel", SMU::class) requiredIf { useSG }

    class SweepPoint(
        val time: Int,
        val interval: Long,
        val useSD: Boolean,
        val sdVoltage: Double,
        val useSG: Boolean,
        val sgVoltage: Double,
        val sdSMU: SMU?,
        val sgSMU: SMU?
    ) : FAction("Hold") {

        var task: RTask? = null

        override fun createPlot(data: ResultTable): FetChPlot {

            return FetChPlot("Hold Voltages", "Time [s]", "Voltage [V]").apply {
                createSeries().watch(data, 0, 1).setName("Source-Drain").setMarkerVisible(false)
                    .setColour(Colour.ORANGERED)
                createSeries().watch(data, 0, 2).setName("Source-Gate").setMarkerVisible(false)
                    .setColour(Colour.CORNFLOWERBLUE)
                isLegendVisible = true
            }

        }

        override fun run(results: ResultTable) {

            val errors = LinkedList<String>()

            if (useSD && sdSMU == null) {
                errors += "Source-Drain Channel is not configured"
            }

            if (useSG && sgSMU == null) {
                errors += "Source-Gate Channel is not configured"
            }

            if (errors.isNotEmpty()) {
                throw Exception(errors.joinToString(", "))
            }

            task = RTask(interval) { t ->

                results.addData(
                    t.secFromStart,
                    sdSMU?.voltage ?: 0.0,
                    sgSMU?.voltage ?: 0.0
                )

            }

            task?.start()

            sdSMU?.turnOff()
            sgSMU?.turnOff()

            sdSMU?.voltage = 0.0
            sgSMU?.voltage = 0.0

            if (useSD) {
                sdSMU?.voltage = sdVoltage
                sdSMU?.turnOn()
            }

            if (useSG) {
                sgSMU?.voltage = sgVoltage
                sgSMU?.turnOn()
            }

            sleep(time)

        }

        override fun onFinish() {
            runRegardless { sdSMU?.turnOff() }
            runRegardless { sgSMU?.turnOff() }
            task?.stop()
        }

        override fun getColumns(): Array<Col> {

            return arrayOf(
                Col("Time", "s"),
                Col("Source-Drain Voltage", "V"),
                Col("Source-Gate Voltage", "V")
            )

        }

        override fun getLabel(): String {
            return "${if (useSD) "SD = $sdVoltage V " else ""}${if (useSG) "SG = $sgVoltage V " else ""}for ${
                Util.msToString(
                    time.toLong()
                )
            }"
        }

    }

    override fun getValues(): List<Int> {
        return Range.step(time, time * (count + 1), time).array().map { it.toInt() }
    }

    override fun generateForValue(value: Int, actions: List<Action<*>>): List<Action<*>> {

        val list = LinkedList<Action<*>>()

        list += MeasurementAction(SweepPoint(time, interval, useSD, sdVoltage, useSG, sgVoltage, sdSMU, sgSMU))
        list += actions

        list.forEach {
            it.setAttribute("S", "${value.toSeconds()} s")
            it.addTag("S = ${Util.msToString(value.toLong())}")
        }

        return list

    }

    override fun formatValue(value: Int): String = Util.msToString(value.toLong())

}