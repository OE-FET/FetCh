package org.oefet.fetch.analysis

import jisa.experiment.Col
import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import java.lang.IllegalArgumentException
import java.util.*

class MeasurementCluster {

    private val measurements = LinkedList<FETMeasurement>()
    private val attributes = LinkedList<String>()

    fun addAttribute(name: String) {

        attributes.add(name)

        for (measurement in measurements) {
            measurement.setAttribute(name, 0.0)
        }

    }

    fun add(measurement: FETMeasurement, vararg values: Double) {

        if (values.size != attributes.size) throw IllegalArgumentException("Must supply all required attributes")

        for (i in attributes.indices) measurement.setAttribute(attributes[i], values[i])
        
        measurements += measurement

    }

    fun getCombined(output: Boolean, transfer: Boolean): ResultTable {

        val columns = Array<Col>(8 + attributes.size) { Col("$it") }

        columns[0] = Col("Temperature", "K")
        columns[1] = Col("SD Voltage", "V")
        columns[2] = Col("SG Voltage", "V")
        columns[3] = Col("Channel Length", "m")
        columns[4] = Col("Channel Width", "m")
        columns[5] = Col("Dielectric Thickness", "m")
        columns[6] = Col("Dielectric Constant")

        for (i in attributes.indices) {
            columns[7 + i] = Col(attributes[i])
        }

        columns[columns.lastIndex] = Col("Mobility", "cm^2/Vs")

        val results = ResultList(*columns)
        val array   = DoubleArray(columns.size)

        for (measurement in measurements) {

            for (curves in measurement) {

                if (curves.transfer != null && transfer) {

                    curves.transfer.calculate()

                    for (row in curves.transfer.fwdMob.flippedCopy()) {

                        array[0] = curves.temperature
                        array[1] = row[1]
                        array[2] = row[0]
                        array[3] = measurement.length
                        array[4] = measurement.width
                        array[5] = measurement.thick
                        array[6] = measurement.dielec

                        for (i in attributes.indices) {
                            array[7 + i] = measurement.getAttribute(attributes[i])
                        }

                        array[array.lastIndex] = row[2]

                        results.addData(*array)

                    }

                    for (row in curves.transfer.bwdMob) {

                        array[0] = curves.temperature
                        array[1] = row[1]
                        array[2] = row[0]
                        array[3] = measurement.length
                        array[4] = measurement.width
                        array[5] = measurement.thick
                        array[6] = measurement.dielec

                        for (i in attributes.indices) {
                            array[7 + i] = measurement.getAttribute(attributes[i])
                        }

                        array[array.lastIndex] = row[2]

                        results.addData(*array)

                    }

                }

                if (curves.output != null && output) {

                    curves.output.calculate()

                    for (row in curves.output.fwdMob.flippedCopy()) {

                        array[0] = curves.temperature
                        array[1] = row[1]
                        array[2] = row[0]
                        array[3] = measurement.length
                        array[4] = measurement.width
                        array[5] = measurement.thick
                        array[6] = measurement.dielec

                        for (i in attributes.indices) {
                            array[7 + i] = measurement.getAttribute(attributes[i])
                        }

                        array[array.lastIndex] = row[2]

                        results.addData(*array)

                    }

                    for (row in curves.output.bwdMob) {

                        array[0] = curves.temperature
                        array[1] = row[1]
                        array[2] = row[0]
                        array[3] = measurement.length
                        array[4] = measurement.width
                        array[5] = measurement.thick
                        array[6] = measurement.dielec

                        for (i in attributes.indices) {
                            array[7 + i] = measurement.getAttribute(attributes[i])
                        }

                        array[array.lastIndex] = row[2]

                        results.addData(*array)

                    }

                }

            }

        }

        return results

    }

}