package org.oefet.fetch

import jisa.experiment.Measurement
import jisa.gui.Plot
import jisa.results.Column
import jisa.results.ResultTable
import org.oefet.fetch.measurement.ACHall
import org.oefet.fetch.measurement.Conductivity
import org.oefet.fetch.measurement.FetChMeasurement
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.ACHallResult
import org.oefet.fetch.results.CondResult
import org.oefet.fetch.results.FetChResult
import org.reflections.Reflections
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.reflect

object Measurements {

    val types = Reflections("org.oefet.fetch.measurement")
        .getSubTypesOf(FetChMeasurement::class.java)
        .map { Config(it.getConstructor().newInstance()) }
        .sortedBy { it.name }

    class Config(private val example: FetChMeasurement) {

        val type   = example.tag
        val name   = example.name
        val mClass = example::class
        val rClass = (example::processResults).reflect()?.returnType?.jvmErasure

        fun createMeasurement(): FetChMeasurement                                             = mClass.primaryConstructor!!.call()
        fun createResult(data: ResultTable): FetChResult = example.processResults(data)
        fun createPlot(data: ResultTable)                                                     = example.createPlot(data)


    }

    fun loadResultFile(data: ResultTable): FetChResult? {

        return types.find { it.type == data.getAttribute("Type") }?.createResult(data) ?: convertFile(data)

    }

    fun createPlot(data: ResultTable): Plot? {

        return types.find { it.type == data.getAttribute("Type") }?.createPlot(data)

    }

    fun createPlot(result: FetChResult): Plot? {
        return types.find { it.rClass == result::class }?.createPlot(result.data)
    }

    fun createPlot(measurement: Measurement): Plot? {
        return types.find { it.mClass == measurement::class }?.createPlot(measurement.results)
    }

    /**
     * Converts files from the old HallSpinner application for use in FetCh
     */
    private fun convertFile(data: ResultTable, extra: List<Quantity<*>> = emptyList()): FetChResult? {

        when (data.getColumn(0).name) {

            "No." -> {

                val newData = ACHall().newResults()
                var startX = 0.0
                var startY = 0.0
                var first = true

                newData.setAttribute("Type", "AC Hall")
                newData.setAttribute("Length", "120E-6 m")
                newData.setAttribute("Width", "60E-6 m")
                newData.setAttribute("Thickness", "37E-9 m")
                newData.setAttribute("FPP Separation", "120E-6 m")
                newData.setAttribute("Dielectric Thickness", "4.0E-7 m")
                newData.setAttribute("Dielectric Permittivity", "2.05")
                newData.setAttribute("Name", "Old Data")
                newData.setAttribute("T", "${data.getMax(data.getColumn(1) as Column<Double>).roundToInt()} K")

                for (row in data) {

                    if (first) {
                        first = false
                        startX = row[8]
                        startY = row[9]
                    }

                    val x = row[8] - startX
                    val y = row[9] - startY
                    val h = sqrt(x.pow(2) + y.pow(2))

                    newData.addData(
                        row[5],
                        row[6],
                        row[4],
                        row[7],
                        0.47093,
                        row[2],
                        row[8] - startX,
                        (data.getMax {it[8] - startX}) * 0.2,
                        row[9] - startY,
                        (data.getMax {it[9] - startY}) * 0.2,
                        h,
                        data.getMax{sqrt((it[8] - startX).pow(2) + (it[9] - startY).pow(2))} * 0.115,
                        row[1]
                    )

                }

                return ACHallResult(newData)

            }

            "T_SP" -> {


                val newData = Conductivity().newResults()

                newData.setAttribute("Type", "FPP Conductivity")
                newData.setAttribute("Length", "120E-6 m")
                newData.setAttribute("Width", "60E-6 m")
                newData.setAttribute("Thickness", "37E-9 m")
                newData.setAttribute("FPP Separation", "120E-6 m")
                newData.setAttribute("Dielectric Thickness", "4.0E-7 m")
                newData.setAttribute("Dielectric Permittivity", "2.05")
                newData.setAttribute("Name", "Old Data")
                newData.setAttribute("T", "${data.getMax(data.getColumn(0) as Column<Double>)} K")

                for (row in data) {

                    newData.addData(row[2], row[5], row[3], row[6], row[2], 0.0, row[1], Double.NaN)

                }

                return CondResult(newData)

            }

            else -> return null

        }

    }

}