package org.oefet.fetch.measurement

import jisa.experiment.Measurement
import org.reflections.Reflections

abstract class FetChMeasurement : Measurement() {

    abstract val type: String

}