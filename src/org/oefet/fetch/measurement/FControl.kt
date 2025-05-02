package org.oefet.fetch.measurement

import jisa.control.PIDController
import jisa.devices.features.InternalFrequencyReference
import jisa.devices.lockin.LockIn
import jisa.devices.power.DCPower
import jisa.control.Sync as Synch

class FControl(private val lockIn: LockIn, private val dcPower: DCPower) : PIDController(100,  lockIn::getFrequency, dcPower::setCurrent) {

    init {
        setPID(0.5, 0.5, 0.5)
    }

    override fun start() {

        if (lockIn is InternalFrequencyReference<*>) {
            lockIn.isInternalReferenceEnabled = true
            lockIn.internalReferenceOscillator.frequency = 0.01
            lockIn.isInternalReferenceEnabled = false
        }

        dcPower.turnOff()
        dcPower.voltage = 10.0
        dcPower.current = 0.0

        dcPower.turnOn()
        super.start()

    }

    override fun stop() {
        dcPower.turnOff()
        super.stop()
    }

    fun waitForStableFrequency(pctRange: Double, time: Int) {
        Synch.waitForParamStable(lockIn::getFrequency, pctRange, time, 1000)
    }

}