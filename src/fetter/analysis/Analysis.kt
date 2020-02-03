package fetter.analysis

import java.util.*

class Analysis {

    private val measurements = LinkedList<FETMeasurement>()

    fun addMeasurement(measurement: FETMeasurement) {
        measurements += measurement
    }

    operator fun plusAssign(measurement: FETMeasurement) {
        addMeasurement(measurement)
    }



}