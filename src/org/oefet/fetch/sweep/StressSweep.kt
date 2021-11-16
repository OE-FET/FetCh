package org.oefet.fetch.sweep

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.SMU
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementAction
import jisa.gui.Colour
import jisa.maths.Range
import jisa.results.Column
import jisa.results.ResultTable
import org.oefet.fetch.action.FetChAction
import org.oefet.fetch.action.VoltageHold.Companion.SD_VOLTAGE
import org.oefet.fetch.action.VoltageHold.Companion.SG_VOLTAGE
import org.oefet.fetch.action.VoltageHold.Companion.TIME
import org.oefet.fetch.gui.elements.FetChPlot
import java.util.*

class StressSweep : FetChSweep<Int>("Stress", "S") {

    var task: RTask? = null

    val interval  by userInput("Basic", "Logging Interval [s]", 0.5) map { it.toMSec().toLong() }
    val time      by userInput("Timing", "Stress Interval [s]", 600.0) map { it.toMSec() }
    val count     by userInput("Timing", "No. Stress Intervals", 10)
    val useSD     by userInput("Source-Drain", "Enabled", false)
    val sdVoltage by userInput("Source-Drain", "Voltage [V]", 50.0)
    val useSG     by userInput("Source-Gate", "Enabled", false)
    val sgVoltage by userInput("Source-Gate", "Voltage [V]", 50.0)

    val gdSMU by optionalInstrument("Ground Channel (SPA)", SMU::class)
    val sdSMU by optionalInstrument("Source-Drain Channel", SMU::class) requiredIf { useSD }
    val sgSMU by optionalInstrument("Source-Gate Channel", SMU::class) requiredIf { useSG }

    override fun getValues(): List<Int> {
        return Range.step(time, time * (count + 1), time).array().map { it.toInt() }
    }

    override fun generateForValue(value: Int, actions: List<Action<*>>): List<Action<*>> {

        val list = LinkedList<Action<*>>()

        list += MeasurementAction(SweepPoint(time, interval, useSD, sdVoltage, useSG, sgVoltage, gdSMU, sdSMU, sgSMU))
        list += actions

        return list

    }

    override fun formatValue(value: Int): String = Util.msToString(value.toLong())

    override fun formatValueForAttribute(value: Int): String = "${value.toSeconds()} s"

    class SweepPoint(
        val time: Int,
        val interval: Long,
        val useSD: Boolean,
        val sdVoltage: Double,
        val useSG: Boolean,
        val sgVoltage: Double,
        val gdSMU: SMU?,
        val sdSMU: SMU?,
        val sgSMU: SMU?
    ) : FetChAction("Hold") {

        var task: RTask? = null

        override fun createDisplay(data: ResultTable): FetChPlot {

            return FetChPlot("Hold Voltages", "Time [s]", "Voltage [V]").apply {
                createSeries().watch(data, TIME, SD_VOLTAGE).setName("Source-Drain").setMarkerVisible(false)
                    .setColour(Colour.ORANGERED)
                createSeries().watch(data, TIME, SG_VOLTAGE).setName("Source-Gate").setMarkerVisible(false)
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

            gdSMU?.turnOff()
            sdSMU?.turnOff()
            sgSMU?.turnOff()

            gdSMU?.voltage = 0.0
            sdSMU?.voltage = 0.0
            sgSMU?.voltage = 0.0

            if (useSD || useSG) {
                gdSMU?.turnOn()
            }

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
            runRegardless { gdSMU?.turnOff() }
            runRegardless { sdSMU?.turnOff() }
            runRegardless { sgSMU?.turnOff() }
            task?.stop()
        }

        override fun getColumns(): Array<Column<*>> {

            return arrayOf(
                TIME,
                SD_VOLTAGE,
                SG_VOLTAGE
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

}