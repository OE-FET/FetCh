package org.oefet.fetch.measurement

import jisa.control.Connection
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

    private val logTasks: MutableList<() -> Double> = ArrayList()
    private val logger: RTask = RTask(2500) { it -> log(log!!, it) }
    private var log: ResultTable? = null;

    fun start(path: String) {

        logTasks.clear()

        val columns = LinkedList<Col>()
        columns.add(Col("Time", "s"))

        for (connection in Connection.getAllConnections()) {

            if (!connection.isConnected) continue

            when (val inst = connection.instrument) {

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
                        columns.add(Col("${inst.javaClass.simpleName} Output $output Heater Power", "%"))
                        logTasks.add { tc.heaterPower }
                    }

                }

                is MSTC -> {

                    for ((sensor, tMeter) in inst.sensors.withIndex()) {
                        columns.add(Col("${inst.javaClass.simpleName} Sensor $sensor Temperature", "K"))
                        logTasks.add { tMeter.temperature }
                    }

                    columns.add(Col("${inst.javaClass.simpleName} Heater Power", "%"))
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

                is EMController -> {

                    columns.add(Col("${inst.javaClass.simpleName} Current"))
                    columns.add(Col("${inst.javaClass.simpleName} Field"))
                    logTasks.add { inst.current }
                    logTasks.add { inst.field }

                }

                is LevelMeter -> {

                    for ((channel, meter) in inst.channels.withIndex()) {

                        columns.add(Col("${inst.javaClass.simpleName} Channel $channel Level"))
                        logTasks.add { meter.level }

                    }

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

        val data = DoubleArray(logTasks.size + 1)
        data[0]  = task.secFromStart

        for ((i, logTask) in logTasks.withIndex()) {
            data[i + 1] = logTask()
        }

        log.addData(*data)

    }

}