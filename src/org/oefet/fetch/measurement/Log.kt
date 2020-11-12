package org.oefet.fetch.measurement

import jisa.control.RTask
import jisa.devices.*
import jisa.experiment.Col
import jisa.experiment.ResultStream
import jisa.experiment.ResultTable
import org.oefet.fetch.gui.tabs.Connections
import org.oefet.fetch.gui.tabs.Dashboard
import java.util.*
import kotlin.collections.ArrayList

object Log {

    private var gdSMU: SMU? = null
    private var sdSMU: SMU? = null
    private var sgSMU: SMU? = null
    private var lockIn: DPLockIn? = null
    private var preAmp: VPreAmp? = null
    private var dcPower: DCPower? = null
    private var tMeter: TMeter? = null
    private var fControl: FControl? = null
    private var tControl: TC? = null
    private var fpp1: VMeter? = null
    private var fpp2: VMeter? = null
    private var tvMeter: VMeter? = null
    private var heater: SMU? = null
    private var logTasks: MutableList<() -> Double> = ArrayList()

    private var log: ResultTable? = null;
    private val logger: RTask = RTask(2500) { it -> log(log!!, it) }

    fun start(path: String) {

        logTasks.clear()

        val columns = LinkedList<Col>()

        for (connection in Connections.getInstrumentsByType(Instrument::class.java)) {

            if (!connection.isConnected) continue

            val inst = connection.get()

            when (inst) {

                is MCSMU -> {

                    for ((channel, smu) in inst.channels.withIndex()) {
                        columns.add(Col("${inst.javaClass.simpleName} Channel $channel Voltage", "V"))
                        columns.add(Col("${inst.javaClass.simpleName} Channel $channel Current", "A"))
                        logTasks.add { smu.voltage }
                        logTasks.add { smu.current }
                    }

                }

                is SMU -> {
                    columns.add(Col("${inst.javaClass.simpleName} Voltage", "V"))
                    columns.add(Col("${inst.javaClass.simpleName} Current", "A"))
                    logTasks.add { inst.voltage }
                    logTasks.add { inst.current }
                }

                is DCPower -> {
                    columns.add(Col("${inst.javaClass.simpleName} Voltage", "V"))
                    columns.add(Col("${inst.javaClass.simpleName} Current", "A"))
                    logTasks.add { inst.voltage }
                    logTasks.add { inst.current }
                }

                is VMeter -> {
                    columns.add(Col("${inst.javaClass.simpleName} Voltage", "V"))
                    logTasks.add { inst.voltage }
                }

                is MSMOTC -> {

                    for ((sensor, tMeter) in inst.sensors.withIndex()) {
                        columns.add(Col("${inst.javaClass.simpleName} Sensor $sensor Temperature", "K"))
                        logTasks.add { tMeter.temperature }
                    }

                    for ((output, tc) in inst.outputs.withIndex()) {
                        columns.add(Col("${inst.javaClass.simpleName} Output $output Heater Power", "K"))
                        logTasks.add { tc.heaterPower }
                    }

                }

                is MSTC -> {

                    for ((sensor, tMeter) in inst.sensors.withIndex()) {
                        columns.add(Col("${inst.javaClass.simpleName} Sensor $sensor Temperature", "K"))
                        logTasks.add { tMeter.temperature }
                    }

                    columns.add(Col("${inst.javaClass.simpleName} Heater Power", "K"))
                    logTasks.add { inst.heaterPower }

                }

                is TC -> {
                    columns.add(Col("${inst.javaClass.simpleName} Temperature", "K"))
                    columns.add(Col("${inst.javaClass.simpleName} Heater Power", "%"))
                    logTasks.add { inst.temperature }
                    logTasks.add { inst.heaterPower }
                }

                is TMeter -> {
                    columns.add(Col("${inst.javaClass.simpleName} Temperature", "K"))
                    logTasks.add { inst.temperature }
                }

                is DPLockIn -> {
                    columns.add(Col("${inst.javaClass.simpleName} X Voltage", "V"))
                    columns.add(Col("${inst.javaClass.simpleName} Y Voltage", "V"))
                    columns.add(Col("${inst.javaClass.simpleName} Frequency", "Hz"))
                    logTasks.add { inst.lockedX }
                    logTasks.add { inst.lockedY }
                    logTasks.add { inst.frequency }
                }

                is LockIn -> {
                    columns.add(Col("${inst.javaClass.simpleName} Voltage", "V"))
                    columns.add(Col("${inst.javaClass.simpleName} Frequency", "Hz"))
                    logTasks.add { inst.lockedAmplitude }
                    logTasks.add { inst.frequency }
                }

            }

        }

        logger.stop()
        log = ResultStream(path, *(columns.toArray(Array(0) { Col("", "") })))
        logger.start()

        Dashboard.watchLog(log!!)

    }

    fun stop() {

        logger.stop()
        log?.finalise()

    }

    private fun log(log: ResultTable, task: RTask) {

        val data = DoubleArray(logTasks.size)

        for ((i, logTask) in logTasks.withIndex()) {
            data[i] = logTask()
        }

        log.addData(*data)

    }

}