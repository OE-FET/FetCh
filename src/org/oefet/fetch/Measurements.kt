package org.oefet.fetch

import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.gui.Plot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.*
import org.oefet.fetch.gui.elements.*
import org.oefet.fetch.measurement.*
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.reflect

object Measurements {

    val types = listOf(
        Config("Output",                      ::Output,        ::OutputResult,   ::OutputPlot),
        Config("Transfer",                    ::Transfer,      ::TransferResult, ::TransferPlot),
        Config("Sync",                        ::VSync,         ::TransferResult, ::SyncPlot),
        Config("FPP Conductivity",            ::Conductivity,  ::CondResult,     ::FPPPlot),
        Config("AC Hall",                     ::ACHall,        ::ACHallResult,   { ACHallPlot(it) }),
        Config("DC Hall",                     ::DCHall,        ::DCHallResult,   ::DCHallPlot),
        Config("Thermal Voltage",             ::TVMeasurement, ::TVResult,       ::TVPlot),
        Config("Thermal Voltage Calibration", ::TVCalibration, ::TVCResult,      ::TVCPlot)
    )

    class Config(val type: String, val measurement: () -> FMeasurement, val result: (ResultTable, List<Quantity>) -> ResultFile, val plot: (ResultTable) -> Plot) {

        private val example = measurement()

        val name   = example.name
        val mClass = example::class
        val rClass = result.reflect()?.returnType?.jvmErasure

        fun createMeasurement(): FMeasurement                                                = measurement()
        fun createResult(data: ResultTable, extra: List<Quantity> = emptyList()): ResultFile = result(data, extra)
        fun createPlot(data: ResultTable)                                                    = plot(data)


    }

    fun loadResultFile(data: ResultTable, extra: List<Quantity> = emptyList()): ResultFile? {

        return types.find { it.type == data.getAttribute("Type") }?.createResult(data, extra) ?: convertFile(
            data,
            extra
        )

    }

    fun createPlot(data: ResultTable): Plot? {

        return types.find { it.type == data.getAttribute("Type") }?.createPlot(data)

    }

    fun createPlot(result: ResultFile): Plot? {
        return types.find { it.rClass == result::class }?.createPlot(result.data)
    }

    fun createPlot(measurement: Measurement): Plot? {
        return types.find { it.mClass == measurement::class }?.createPlot(measurement.results)
    }

    private fun convertFile(data: ResultTable, extra: List<Quantity> = emptyList()): ResultFile? {

        when (data.getName(0)) {

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
                        (data.getMax {it[8] - startX}) * 0.2,
                        row[9] - startY,
                        (data.getMax {it[9] - startY}) * 0.2,
                        h,
                        data.getMax{sqrt((it[8] - startX).pow(2) + (it[9] - startY).pow(2))} * 0.115,
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

            else -> return null

        }

    }

}