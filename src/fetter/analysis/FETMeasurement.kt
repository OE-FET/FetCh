package fetter.analysis

import jisa.Util
import jisa.experiment.ResultList
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class FETMeasurement(private val name: String, path: String) : Iterable<FETMeasurement.TPoint> {

    private val transferCurves = LinkedHashMap<Double, TCurve>()
    private val outputCurves   = LinkedHashMap<Double, OCurve>()
    private val directory      = File(path)
    private val temperatures   = LinkedList<Double>()
    private val attributes     = LinkedHashMap<String, Double>()
    val length : Double
    val width  : Double
    val thick  : Double
    val dielec : Double

    init {

        val infoFile = File(Util.joinPath(path, "$name-info.txt"))

        if (!infoFile.exists()) {
            throw Exception("Cannot find device info for specified measurement")
        }

        infoFile.bufferedReader().run {
            length = readLine().removePrefix("Length: ").toDoubleOrNull() ?: 40e-6
            width  = readLine().removePrefix("Width: ").toDoubleOrNull() ?: 1000e-6
            thick  = readLine().removePrefix("Dielectric Thickness: ").toDoubleOrNull() ?: 400e-9
            dielec = readLine().removePrefix("Dielectric Constant: ").toDoubleOrNull() ?: 2.05
            close()
        }

        val files = directory.listFiles()

        if (files != null) {

            val temps = HashMap<Double, String>()

            for (found in files) {

                val match = Regex("$name(?:-([0-9]+\\.?[0-9]*)K)?-(Transfer|Output)\\.csv").matchEntire(found.name) ?: continue
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

                if (outputFile.exists())   outputCurves[t]   = OCurve(length, width, dielec * EPSILON / thick, ResultList.loadFile(outputFile.absolutePath))
                if (transferFile.exists()) transferCurves[t] = TCurve(length, width, dielec * EPSILON / thick, ResultList.loadFile(transferFile.absolutePath))

            }


        }

    }

    fun getTemperatures(): List<Double> = LinkedList<Double>(temperatures)

    fun hasOutputCurve(temperature: Double): Boolean = outputCurves.containsKey(temperature)

    fun hasTransferCurve(temperature: Double): Boolean = transferCurves.containsKey(temperature)

    fun getOutputCurve(temperature: Double): OCurve = outputCurves[temperature]
        ?: throw IndexOutOfBoundsException("No output curve for that temperature exists")

    fun getTransferCurve(temperature: Double): TCurve = transferCurves[temperature]
        ?: throw IndexOutOfBoundsException("No transfer curve for that temperature exists")

    fun hasAttribute(key: String): Boolean = attributes.containsKey(key)

    fun getAttribute(key: String): Double = attributes[key] ?: throw IndexOutOfBoundsException()

    fun setAttribute(key: String, value: Number) {
        attributes[key] = value.toDouble()
    }

    class TPoint(val temperature: Double, val output: OCurve?, val transfer: TCurve?)

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

    companion object {
        const val EPSILON = 8.85418782e-12;
    }

}