package org.oefet.fetch.measurement

import jisa.Util
import jisa.control.Connection
import jisa.control.RTask
import jisa.devices.camera.Camera
import jisa.devices.electromagnet.EMController
import jisa.devices.lockin.DPLockIn
import jisa.devices.lockin.LockIn
import jisa.devices.meter.MSTMeter
import jisa.devices.meter.TMeter
import jisa.devices.meter.VMeter
import jisa.devices.pid.PID
import jisa.devices.power.DCPower
import jisa.devices.relay.MSwitch
import jisa.devices.smu.MCSMU
import jisa.devices.smu.SMU
import jisa.devices.smu.SPA
import jisa.results.Column
import jisa.results.ResultStream
import jisa.results.ResultTable
import org.oefet.fetch.Settings
import org.oefet.fetch.gui.tabs.Dashboard
import java.util.*

object Log {

    private val logTasks: MutableList<() -> Double> = ArrayList()

    private val logger: RTask = RTask(
        if (Settings.hasValue("loggerInterval")) {
            Settings.intValue("loggerInterval").get().toLong()
        } else {
            2500
        }
    ) { it -> log(log!!, it) }

    private var log: ResultTable? = null
    private val map: Map<String, Boolean> = HashMap()

    fun start(path: String) {

        logTasks.clear()

        val columns = LinkedList<Column<*>>()
        columns.add(Column.ofLongs("Time", "UTC ms"))

        for (connection in Connection.getAllConnections()) {

            if (!connection.isConnected) continue
            val inst = connection.instrument
            val name = "${connection.name} (${inst?.javaClass?.simpleName ?: "NULL"})"

            when (inst) {


                is MCSMU<*> -> {

                    for (smu in inst[SMU::class]) {
                        columns.add(Column.ofDoubles("$name ${smu.name} Voltage", "V"))
                        columns.add(Column.ofDoubles("$name ${smu.name} Current", "A"))
                        logTasks.add { smu.getVoltage() }
                        logTasks.add { smu.getCurrent() }
                    }

                }

                is SPA<*, *, *, *> -> {

                    for (smu in inst.smuChannels) {
                        columns.add(Column.ofDoubles("$name ${smu.name} Voltage", "V"))
                        columns.add(Column.ofDoubles("$name ${smu.name} Current", "A"))
                        logTasks.add { smu.getVoltage() }
                        logTasks.add { smu.getCurrent() }
                    }

                    for (vs in inst.vSourceChannels) {
                        columns.add(Column.ofDoubles("$name ${vs.name} Voltage", "V"))
                        logTasks.add { vs.getVoltage() }
                    }

                    for (vm in inst.vMeterChannels) {
                        columns.add(Column.ofDoubles("$name ${vm.name} Voltage", "V"))
                        logTasks.add { vm.getVoltage() }
                    }

                    for (sw in inst.switchChannels) {
                        columns.add(Column.ofDoubles("$name ${sw.name} State"))
                        logTasks.add { if (sw.isOn) 1.0 else 0.0 }
                    }

                }

                is SMU -> {

                    columns.add(Column.ofDoubles("$name Voltage", "V"))
                    columns.add(Column.ofDoubles("$name Current", "A"))
                    logTasks.add { inst.getVoltage() }
                    logTasks.add { inst.getCurrent() }

                }

                is DCPower -> {

                    columns.add(Column.ofDoubles("$name Voltage", "V"))
                    columns.add(Column.ofDoubles("$name Current", "A"))
                    logTasks.add { inst.voltage }
                    logTasks.add { inst.current }

                }

                is VMeter -> {

                    columns.add(Column.ofDoubles("$name Voltage", "V"))
                    logTasks.add { inst.getVoltage() }

                }

                is PID -> {

                    for (input in inst.inputs) {
                        columns.add(Column.ofDoubles("$name ${input.name} ${input.valueName}", input.units))
                        logTasks.add { input.value }
                    }

                    for (output in inst.outputs) {
                        columns.add(Column.ofDoubles("$name ${output.name} ${output.valueName}", output.units))
                        logTasks.add { output.value }
                    }

                }

                is MSTMeter<*> -> {

                    for (tMeter in inst[TMeter::class]) {
                        columns.add(Column.ofDoubles("$name ${tMeter.name} Temperature", "K"))
                        logTasks.add { tMeter.temperature }
                    }

                }

                is TMeter -> {

                    columns.add(Column.ofDoubles("$name Temperature", "K"))
                    logTasks.add { inst.temperature }

                }

                is DPLockIn -> {

                    columns.add(Column.ofDoubles("$name X Voltage", "V"))
                    columns.add(Column.ofDoubles("$name Y Voltage", "V"))
                    columns.add(Column.ofDoubles("$name Frequency", "Hz"))
                    logTasks.add { inst.lockedX }
                    logTasks.add { inst.lockedY }
                    logTasks.add { inst.frequency }

                }

                is LockIn -> {

                    columns.add(Column.ofDoubles("$name Voltage", "V"))
                    columns.add(Column.ofDoubles("$name Frequency", "Hz"))
                    logTasks.add { inst.lockedAmplitude }
                    logTasks.add { inst.frequency }

                }

                is EMController -> {

                    columns.add(Column.ofDoubles("$name Current"))
                    columns.add(Column.ofDoubles("$name Field"))
                    logTasks.add { inst.current }
                    logTasks.add { inst.field }

                }

                is MSwitch<*> -> {

                    for (channel in inst.switches) {
                        columns.add(Column.ofDoubles("$name ${channel.name} State"))
                        logTasks.add { if (channel.isOn) 1.0 else 0.0 }
                    }

                }

                is jisa.devices.relay.Switch -> {

                    columns.add(Column.ofDoubles("$name State"))
                    logTasks.add { if (inst.isOn) 1.0 else 0.0 }

                }

                is Camera<*> -> {
                    columns.add(Column.ofDoubles("$name Acquisition Framerate", "Hz"))
                    columns.add(Column.ofDoubles("$name Processing Framerate", "Hz"))
                    logTasks.add { inst.acquisitionFPS }
                    logTasks.add { inst.processingFPS }
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