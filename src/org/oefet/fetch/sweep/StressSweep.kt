package org.oefet.fetch.sweep

import jisa.Util
import jisa.control.RTask
import jisa.devices.interfaces.SMU
import jisa.enums.Icon
import jisa.experiment.queue.Action
import jisa.experiment.queue.MeasurementAction
import jisa.experiment.queue.SimpleAction
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

class StressSweep : FetChSweep<Int>("Stress", "S", Icon.CLOCK.blackImage) {

    var task: RTask? = null

    val interval  by userTimeInput("Basic", "Logging Interval", 500)
    val time      by userTimeInput("Timing", "Stress Interval", 600000)
    val count     by userInput("Timing", "No. Stress Intervals", 10)
    val useSD     by userInput("Source-Drain", "Enabled", false)
    val sdVoltage by userInput("Source-Drain", "Voltage [V]", 50.0)
    val offSD     by userChoice("Source-Drain", "Auto Turn Off", "At each step", "Only at end", "Never")
    val useSG     by userInput("Source-Gate", "Enabled", false)
    val sgVoltage by userInput("Source-Gate", "Voltage [V]", 50.0)
    val offSG     by userChoice("Source-Gate", "Auto Turn Off", "At each step", "Only at end", "Never")

    val gdSMU by optionalInstrument("Ground Channel (SPA)", SMU::class)
    val sdSMU by optionalInstrument("Source-Drain Channel", SMU::class) requiredIf { useSD }
    val sgSMU by optionalInstrument("Source-Gate Channel", SMU::class) requiredIf { useSG }

    companion object {
        const val AUTO_OFF_ALL  = 0;
        const val AUTO_OFF_END  = 1;
        const val AUTO_OFF_NONE = 2;
    }

    override fun getValues(): List<Int> {
        return Range.step(time, time * (count + 1), time).array().map { it.toInt() }
    }

    override fun generateForValue(value: Int, actions: List<Action<*>>): List<Action<*>> {

        val list = LinkedList<Action<*>>()

        list += MeasurementAction(SweepPoint(time, interval.toLong(), useSD, sdVoltage, useSG, sgVoltage, gdSMU, sdSMU, sgSMU, offSD, offSG))

        if (useSD && offSD == AUTO_OFF_ALL) {
            list += SimpleAction("Turn Off SD Channel") {
                sdSMU?.turnOff()
            }
        }

        if (useSG && offSG == AUTO_OFF_ALL) {
            list += SimpleAction("Turn Off SG Channel") {
                sgSMU?.turnOff()
            }
        }

        list += actions

        return list

    }

    override fun generateFinalActions(): List<Action<*>> {

        val list = ArrayList<Action<*>>()

        if (useSD && offSD in arrayOf(AUTO_OFF_END, AUTO_OFF_ALL)) {
            list += SimpleAction("Turn Off SD Channel") {
                sdSMU?.turnOff()
            }
        }

        if (useSG && offSG in arrayOf(AUTO_OFF_END, AUTO_OFF_ALL)) {
            list += SimpleAction("Turn Off SG Channel") {
                sgSMU?.turnOff()
            }
        }

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
        val sgSMU: SMU?,
        val offSD: Int,
        val offSG: Int
    ) : FetChAction("Hold", Icon.CLOCK.blackImage) {

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

            if (useSD || useSG) {
                gdSMU?.turnOn()
                gdSMU?.voltage = 0.0
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