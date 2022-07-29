package org.oefet.fetch.measurement

import jisa.Util
import jisa.control.Connection
import jisa.control.RTask
import jisa.devices.interfaces.*
import jisa.results.*
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.tabs.Dashboard
import java.util.*

object Log {

    private val logTasks: MutableList<() -> Double> = ArrayList()
    private val logger: RTask = RTask(
        if (Settings.hasValue("loggerInterval")) Settings.intValue("loggerInterval").get().toLong() else 2500
    ) { it -> log(log!!, it) }
    private var log: ResultTable? = null
    private val map: Map<String, Boolean> = HashMap()

    fun start(path: String) {

        logTasks.clear()

        val columns = LinkedList<Column<*>>()
        columns.add(LongColumn("Time", "UTC ms"))

        for (connection in Connection.getAllConnections()) {

            if (!connection.isConnected) continue
            val inst = connection.instrument
            val name = "${connection.name} (${inst?.javaClass?.simpleName ?: "NULL"})"

            when (inst) {


                is MCSMU        -> {

                    for (smu in inst.channels) {
                        columns.add(DoubleColumn("$name ${smu.channelName} Voltage", "V"))
                        columns.add(DoubleColumn("$name ${smu.channelName} Current", "A"))
                        logTasks.add { smu.getVoltage() }
                        logTasks.add { smu.getCurrent() }
                    }

                }

                is SMU          -> {

                    columns.add(DoubleColumn("$name Voltage", "V"))
                    columns.add(DoubleColumn("$name Current", "A"))
                    logTasks.add { inst.getVoltage() }
                    logTasks.add { inst.getCurrent() }

                }

                is DCPower      -> {

                    columns.add(DoubleColumn("$name Voltage", "V"))
                    columns.add(DoubleColumn("$name Current", "A"))
                    logTasks.add { inst.voltage }
                    logTasks.add { inst.current }

                }

                is VMeter       -> {

                    columns.add(DoubleColumn("$name Voltage", "V"))
                    logTasks.add { inst.getVoltage() }

                }

                is PID           -> {

                    for (input in inst.inputs) {
                        columns.add(DoubleColumn("$name ${input.name} ${input.valueName}", input.units))
                        logTasks.add { input.value }
                    }

                    for (output in inst.outputs) {
                        columns.add(DoubleColumn("$name ${output.name} ${output.valueName}", output.units))
                        logTasks.add { output.value }
                    }

                }

                is MSTMeter     -> {

                    for (tMeter in inst.sensors) {
                        columns.add(DoubleColumn("$name ${tMeter.sensorName} Temperature", "K"))
                        logTasks.add { tMeter.temperature }
                    }

                }

                is TMeter       -> {

                    columns.add(DoubleColumn("$name Temperature", "K"))
                    logTasks.add { inst.temperature }

                }

                is DPLockIn     -> {

                    columns.add(DoubleColumn("$name X Voltage", "V"))
                    columns.add(DoubleColumn("$name Y Voltage", "V"))
                    columns.add(DoubleColumn("$name Frequency", "Hz"))
                    logTasks.add { inst.lockedX }
                    logTasks.add { inst.lockedY }
                    logTasks.add { inst.frequency }

                }

                is LockIn       -> {

                    columns.add(DoubleColumn("$name Voltage", "V"))
                    columns.add(DoubleColumn("$name Frequency", "Hz"))
                    logTasks.add { inst.lockedAmplitude }
                    logTasks.add { inst.frequency }

                }

                is EMController -> {

                    columns.add(DoubleColumn("$name Current"))
                    columns.add(DoubleColumn("$name Field"))
                    logTasks.add { inst.current }
                    logTasks.add { inst.field }

                }

                is LevelMeter   -> {

                    for (meter in inst.channels) {

                        columns.add(DoubleColumn("$name ${meter.getChannelName(0)} Level"))
                        logTasks.add { meter.level }

                    }

                }

                is MSwitch      -> {

                    for (channel in inst.channels) {
                        columns.add(DoubleColumn("$name ${channel.channelName} State"))
                        logTasks.add { if (channel.isOn) 1.0 else 0.0 }
                    }

                }

                is Switch       -> {

                    columns.add(DoubleColumn("$name State"))
                    logTasks.add { if (inst.isOn) 1.0 else 0.0 }

                }

            }

        }

        logger.stop()
        log = ResultStream(path, *(columns.toArray(Array<Column<*>?>(0) { null })))
        logger.start()

        Dashboard.watchLog(log!!)

    }

    fun stop() {
        logger.stop()
    }


    var interval: Int
        get() {
            return logger.interval.toInt()
        }
        set(value) {
            logger.interval = value.toLong()
            Settings.intValue("loggerInterval").set(value)
        }

    val isRunning: Boolean
        get() {
            return logger.isRunning
        }

    private fun log(log: ResultTable, task: RTask) {

        try {

            val data = Array<Any>(logTasks.size + 1) { 0.0 }
            data[0] = System.currentTimeMillis()

            for ((i, logTask) in logTasks.withIndex()) {

                if (Dashboard.isLogged(i)) {

                    data[i + 1] = try {
                        logTask()
                    } catch (e: Exception) {
                        Double.NaN
                    }

                } else {

                    data[i + 1] = Double.NaN

                }

            }

            log.addData(*data)

            if ((task.count % 500) == 0) {
                Util.runAsync { System.gc() }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}