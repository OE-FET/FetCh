package org.oefet.fetch.measurement

import jisa.Util
import jisa.control.Connection
import jisa.control.RTask
import jisa.devices.interfaces.*
import jisa.experiment.Col
import jisa.experiment.ResultStream
import jisa.experiment.ResultTable
import org.oefet.fetch.gui.tabs.Dashboard
import java.util.*
import kotlin.collections.ArrayList

object Log {

    private val logTasks: MutableList<() -> Double> = ArrayList()
    private val logger: RTask = RTask(2500) { it -> log(log!!, it) }
    private var log: ResultTable? = null

    fun start(path: String) {

        logTasks.clear()

        val columns = LinkedList<Col>()
        columns.add(Col("Time", "s"))

        for (connection in Connection.getAllConnections()) {

            if (!connection.isConnected) continue
            val inst = connection.instrument
            val name = "${connection.name} (${inst?.javaClass?.simpleName ?: "NULL"})"

            when (inst) {


                is MCSMU        -> {

                    for (smu in inst.channels) {
                        columns.add(Col("$name ${smu.channelName} Voltage", "V"))
                        columns.add(Col("$name ${smu.channelName} Current", "A"))
                        logTasks.add { smu.getVoltage(2e-3) }
                        logTasks.add { smu.getCurrent(2e-3) }
                    }

                }

                is SMU          -> {

                    columns.add(Col("$name Voltage", "V"))
                    columns.add(Col("$name Current", "A"))
                    logTasks.add { inst.getVoltage(2e-3) }
                    logTasks.add { inst.getCurrent(2e-3) }

                }

                is DCPower      -> {

                    columns.add(Col("$name Voltage", "V"))
                    columns.add(Col("$name Current", "A"))
                    logTasks.add { inst.voltage }
                    logTasks.add { inst.current }

                }

                is VMeter       -> {

                    columns.add(Col("$name Voltage", "V"))
                    logTasks.add { inst.getVoltage(2e-3) }

                }

                is MSMOTC       -> {

                    for (tMeter in inst.sensors) {
                        columns.add(Col("$name ${tMeter.sensorName} Temperature", "K"))
                        logTasks.add { tMeter.temperature }
                    }

                    for (tc in inst.outputs) {
                        columns.add(Col("$name ${tc.outputName} Heater Power", "%"))
                        logTasks.add { tc.heaterPower }
                    }

                }

                is MSTC         -> {

                    for (tMeter in inst.sensors) {
                        columns.add(Col("$name ${tMeter.sensorName} Temperature", "K"))
                        logTasks.add { tMeter.temperature }
                    }

                    columns.add(Col("$name Heater Power", "%"))
                    logTasks.add { inst.heaterPower }

                }

                is TC           -> {

                    columns.add(Col("$name Temperature", "K"))
                    columns.add(Col("$name Heater Power", "%"))
                    logTasks.add { inst.temperature }
                    logTasks.add { inst.heaterPower }

                }

                is MSTMeter -> {

                    for (tMeter in inst.sensors) {
                        columns.add(Col("$name ${tMeter.sensorName} Temperature", "K"))
                        logTasks.add { tMeter.temperature }
                    }

                }

                is TMeter       -> {

                    columns.add(Col("$name Temperature", "K"))
                    logTasks.add { inst.temperature }

                }

                is DPLockIn     -> {

                    columns.add(Col("$name X Voltage", "V"))
                    columns.add(Col("$name Y Voltage", "V"))
                    columns.add(Col("$name Frequency", "Hz"))
                    logTasks.add { inst.lockedX }
                    logTasks.add { inst.lockedY }
                    logTasks.add { inst.frequency }

                }

                is LockIn       -> {

                    columns.add(Col("$name Voltage", "V"))
                    columns.add(Col("$name Frequency", "Hz"))
                    logTasks.add { inst.lockedAmplitude }
                    logTasks.add { inst.frequency }

                }

                is EMController -> {

                    columns.add(Col("$name Current"))
                    columns.add(Col("$name Field"))
                    logTasks.add { inst.current }
                    logTasks.add { inst.field }

                }

                is LevelMeter   -> {

                    for (meter in inst.channels) {

                        columns.add(Col("$name ${meter.getChannelName(0)} Level"))
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
        data[0] = task.secFromStart

        for ((i, logTask) in logTasks.withIndex()) {
            data[i + 1] = try {
                logTask()
            } catch (e: Exception) {
                Double.NaN
            }
        }

        log.addData(*data)

        if ((task.count % 500) == 0) {
            Util.runAsync { System.gc() }
        }

    }

}