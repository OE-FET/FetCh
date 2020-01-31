package fetter.analysis

import jisa.Util
import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class FETMeasurement(private val name: String, path: String) : Iterable<FETMeasurement.TPoint> {

    private val transferCurves = LinkedHashMap<Double, ResultTable>()
    private val outputCurves = LinkedHashMap<Double, ResultTable>()
    private val directory = File(path)
    private val temperatures = LinkedList<Double>()
    private val attributes = LinkedHashMap<String, String>()

    init {

        val files = directory.listFiles { _, n ->
            n.matches(Regex("$name(?:-([0-9]+\\.?[0-9]*)K)?-(Transfer|Output)\\.csv"))
        }

        if (files != null) {

            val temps = HashMap<Double, String>()

            for (found in files) {

                val match = Regex("$name(?:-([0-9]+\\.?[0-9]*)K)?-(Transfer|Output)\\.csv").find(found.name) ?: continue
                val temp = if (match.groupValues[1].isBlank()) "-1" else match.groupValues[1]
                temps[temp.toDouble()] = temp

            }

            for ((t, temp) in temps.toSortedMap()) {

                temperatures += t

                val outputFile = File(
                    Util.joinPath(
                        directory.absolutePath,
                        if (t > -1) "$name-${temp}K-Output.csv" else "$name-Output.csv"
                    )
                )
                val transferFile = File(
                    Util.joinPath(
                        directory.absolutePath,
                        if (t > -1) "$name-${temp}K-Transfer.csv" else "$name-Transfer.csv"
                    )
                )

                if (outputFile.exists()) outputCurves[t] = ResultList.loadFile(outputFile.absolutePath)
                if (transferFile.exists()) transferCurves[t] = ResultList.loadFile(transferFile.absolutePath)

            }


        }

    }

    fun getTemperatures(): List<Double> = LinkedList<Double>(temperatures)

    fun hasOutputCurve(temperature: Double): Boolean = outputCurves.containsKey(temperature)

    fun hasTransferCurve(temperature: Double): Boolean = transferCurves.containsKey(temperature)

    fun getOutputCurve(temperature: Double): ResultTable = outputCurves[temperature]
        ?: throw IndexOutOfBoundsException("No output curve for that temperature exists")

    fun getTransferCurve(temperature: Double): ResultTable = transferCurves[temperature]
        ?: throw IndexOutOfBoundsException("No transfer curve for that temperature exists")

    fun hasAttribute(key: String): Boolean = attributes.containsKey(key)

    fun getAttribute(key: String): String = attributes[key] ?: throw IndexOutOfBoundsException()

    fun setAttribute(key: String, value: String) {
        attributes[key] = value
    }

    class TPoint(val temperature: Double, val output: ResultTable?, val transfer: ResultTable?)

    override fun iterator(): Iterator<TPoint> = object : Iterator<TPoint> {

        private val iterator = temperatures.iterator()

        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }

        override fun next(): TPoint {
            val temp = iterator.next()
            return TPoint(
                temp,
                if (hasOutputCurve(temp)) getOutputCurve(temp) else null,
                if (hasTransferCurve(temp)) getTransferCurve(temp) else null
            )
        }

    }

}