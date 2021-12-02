package org.oefet.fetch.measurement

import jisa.control.PIDController
import jisa.control.Synch
import jisa.devices.interfaces.DCPower
import jisa.devices.interfaces.LockIn

class FControl(private val lockIn: LockIn, private val dcPower: DCPower) : PIDController(100,  lockIn::getFrequency, dcPower::setCurrent) {

    init {
        setPID(0.5, 0.5, 0.5)
    }

    override fun start() {

        lockIn.refMode = LockIn.RefMode.INTERNAL
        lockIn.setOscFrequency(0.01)
        lockIn.refMode = LockIn.RefMode.EXTERNAL

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