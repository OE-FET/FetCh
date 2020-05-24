package org.oefet.fetch.measurement

import jisa.experiment.Measurement
import org.reflections.Reflections

abstract class FetChMeasurement : Measurement() {

    abstract val type: String

    abstract fun loadInstruments(instruments: Instruments)

    companion object {

        val types = Reflections("org.oefet.fetch.measurement").getSubTypesOf(FetChMeasurement::class.java).map { Type(it.getConstructor().newInstance()) }

    }

    class Type(private val measurement: FetChMeasurement) {

        val name: String = measurement.name

        fun create(): FetChMeasurement = measurement.javaClass.getConstructor().newInstance()

    }

}