package org.oefet.fetch.analysis.results

import javafx.scene.image.Image
import jisa.experiment.ResultTable
import jisa.gui.Plot
import org.oefet.fetch.analysis.quantities.Quantity
import org.oefet.fetch.analysis.UnknownResultException
import org.oefet.fetch.measurement.ACHall
import org.oefet.fetch.measurement.Conductivity
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

interface ResultFile {

    val data: ResultTable
    val parameters: MutableList<Quantity>
    val quantities: MutableList<Quantity>
    val plot: Plot
    val name: String
    val image: Image
    val label: String

    fun calculateHybrids(quantities: List<Quantity>) : List<Quantity>

    fun getParameterString(): String {

        val parts = LinkedList<String>()

        for (parameter in parameters) {

            if (parameter.extra) parts += "%s = %s %s".format(parameter.name, parameter.value, parameter.unit)

        }

        return parts.joinToString(", ")

    }

    companion object {

        fun loadData(data: ResultTable, extra: List<Quantity> = emptyList()) : ResultFile {

            return when (data.getAttribute("Type")) {

                "Transfer"         -> TransferResult(data, extra)
                "Output"           -> OutputResult(data, extra)
                "FPP Conductivity" -> CondResult(data, extra)
                "AC Hall"          -> ACHallResult(data, extra)
                else               -> convertFile(data, extra)

            } ?: throw UnknownResultException("Unknown measurement type")

        }

        private fun convertFile(data: ResultTable, extra: List<Quantity> = emptyList()): ResultFile? {

            when(data.getName(0)) {

                "No."           -> {

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
                    newData.setAttribute("T", "${data.getMax(1).roundToInt()} K")

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
                            1.0e-12,
                            row[9] - startY,
                            1.0e-12,
                            h,
                            1.0e-12,
                            row[1]
                        )

                    }

                    return ACHallResult(newData, extra)

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
                    newData.setAttribute("T", "${data.getMax(0)} K")

                    for (row in data) {

                        newData.addData(row[2], row[5], row[3], row[6], row[2], 0.0, row[1], Double.NaN)

                    }

                    return CondResult(newData, extra)

                }

                else                               -> return null

            }

        }

    }

}