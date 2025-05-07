package org.oefet.fetch.logging

import jisa.control.Connection
import jisa.control.RTask
import jisa.devices.MultiInstrument
import jisa.devices.camera.Camera
import jisa.devices.lockin.DPLockIn
import jisa.devices.lockin.LockIn
import jisa.devices.meter.FMeter
import jisa.devices.meter.IMeter
import jisa.devices.meter.TMeter
import jisa.devices.meter.VMeter
import jisa.devices.pid.PID
import jisa.devices.source.FSource
import jisa.devices.source.ISource
import jisa.devices.source.VSource
import java.util.*

object Log {

    val sources  = LinkedList<Source>()
    var interval = 1000L
    var task     = RTask(interval, this::tick)
    val loggers  = LinkedList<Dash>()

    fun start() {
        sources.forEach { it.checkUse() }
        task = RTask(interval, this::tick)
        task.start()
    }

    fun stop() {
        task.stop()
    }

    fun populateSources() {

        sources.clear()

        for (connection in Connection.getAllConnections().filter { it.isConnected }) {

            val instrument = connection.instrument

            val all = if (instrument is MultiInstrument) {
                listOf(instrument) + instrument.subInstruments
            } else {
                listOf(instrument)
            }

            for (inst in all) {

                val name = if (inst == instrument) {
                    "${connection.name} (${instrument.name})"
                } else {
                    "${connection.name} (${instrument.name}) ${inst.name}"
                }

                if (inst is VMeter) {
                    sources += Source("$name Voltage", "V") { inst.voltage }
                } else if (inst is VSource) {
                    sources += Source("$name Voltage", "V") { inst.voltage }
                }

                if (inst is IMeter) {
                    sources += Source("$name Current", "A") { inst.current }
                } else if (inst is ISource) {
                    sources += Source("$name Current", "A") { inst.current }
                }

                if (inst is TMeter) {
                    sources += Source("$name Temperature", "K") { inst.temperature }
                } else if (inst is PID.Input) {
                    sources += Source("$name Input ${inst.valueName}", inst.units) { inst.value }
                }

                if (inst is PID.Output) {
                    sources += Source("$name Output ${inst.valueName}", inst.units) { inst.value }
                }

                if (inst is FMeter) {
                    sources += Source("$name Frequency", "Hz") { inst.frequency }
                } else if (inst is FSource) {
                    sources += Source("$name Frequency", "Hz") { inst.frequency }
                }

                if (inst is LockIn) {

                    if (inst is DPLockIn) {
                        sources += Source("$name X Voltage", "V") { inst.lockedX }
                        sources += Source("$name Y Voltage", "V") { inst.lockedY }
                    }

                    sources += Source("$name Amplitude Voltage", "V") { inst.lockedAmplitude }

                }

                if (inst is Camera<*>) {
                    sources += Source("$name Acquisition Framerate", "Hz") { inst.acquisitionFPS }
                    sources += Source("$name Processing Framerate", "Hz") { inst.processingFPS }
                }

            }

        }

        sources.sortBy { it.name }
        loggers.flatMap { it.values }.forEach { it.refreshSources() }

    }

    fun tick() {

        val time = System.currentTimeMillis()
        updateSources()
        loggers.parallelStream().filter{ it.isEnabled }.forEach { it.logStep(time) }

    }

    fun updateSources() {
        sources.parallelStream().filter { it.isUsed }.forEach { it.update() }
    }

    fun getSources(): List<Source> = sources.toList()

}