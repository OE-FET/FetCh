package org.oefet.fetch.measurement

import jisa.control.PIDController
import jisa.devices.DCPower
import jisa.devices.LockIn

class FControl(private val lockIn: LockIn, private val dcPower: DCPower) : PIDController(100,  lockIn::getFrequency, dcPower::setCurrent) {

    init {
        setPID(0.5, 0.5, 0.5)
    }

    override fun start() {

        lockIn.setRefMode(LockIn.RefMode.INTERNAL)
        lockIn.setOscFrequency(0.01)
        lockIn.setRefMode(LockIn.RefMode.EXTERNAL)

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

}