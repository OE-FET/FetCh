package org.oefet.fetch.results

import jisa.experiment.ResultList
import jisa.experiment.ResultTable
import jisa.maths.interpolation.Interpolation
import org.oefet.fetch.*
import org.oefet.fetch.quantities.*
import org.oefet.fetch.gui.elements.OutputPlot
import org.oefet.fetch.gui.images.Images
import org.oefet.fetch.measurement.Output
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class OutputResult(override val data: ResultTable, extraParams: List<Quantity> = emptyList()) : ResultFile {

    val SET_SD_VOLTAGE = data.findColumn(Output.SET_SD_VOLTAGE)
    val SET_SG_VOLTAGE = data.findColumn(Output.SET_SG_VOLTAGE)
    val SD_VOLTAGE     = data.findColumn(Output.SD_VOLTAGE)
    val SD_CURRENT     = data.findColumn(Output.SD_CURRENT)
    val SG_VOLTAGE     = data.findColumn(Output.SG_VOLTAGE)
    val SG_CURRENT     = data.findColumn(Output.SG_CURRENT)
    val FPP_1          = data.findColumn(Output.FPP_1)
    val FPP_2          = data.findColumn(Output.FPP_2)
    val TEMPERATURE    = data.findColumn(Output.TEMPERATURE)
    val GROUND_CURRENT = data.findColumn(Output.GROUND_CURRENT)

    override val parameters = ArrayList<Quantity>()
    override val quantities = ArrayList<Quantity>()
    override val plot       = OutputPlot(data).apply { legendRows = data.getUniqueValues(SET_SG_VOLTAGE).size }
    override val name       = "Output Measurement (${data.getAttribute("Name")})"
    override val image      = Images.getImage("output.png")
    override val label      = "Output"

    override var length:       Double = 0.0
    override var separation:   Double = 0.0
    override var width:        Double = 0.0
    override var thickness:    Double = 0.0
    override var dielectric:   Double = 0.0
    override var permittivity: Double = 0.0
    override var temperature:  Double = Double.NaN
    override var repeat:       Double = 0.0
    override var stress:       Double = 0.0
    override var field:        Double = 0.0

    private val possibleParameters = listOf(
        Device::class,
        Temperature::class,
        Repeat::class,
        Time::class,
        Length::class,
        FPPSeparation::class,
        Width::class,
        Thickness::class,
        DThickness::class,
        Permittivity::class,
        Gate::class,
        Drain::class
    )

    init {

        parseParameters(data, extraParams, data.getMean(TEMPERATURE))

        val capacitance = permittivity * EPSILON / dielectric

        try {

            val fwd = ResultList("V", "G", "I")
            val bwd = ResultList("V", "G", "I")
            val SDV = 0
            val SGV = 1
            val SDI = 2

            for ((gate, data) in data.split(SET_SG_VOLTAGE)) {

                val fb = data.splitTwoWaySweep { it[SET_SD_VOLTAGE] }

                for (row in fb.forward)  fwd.addData(row[SET_SD_VOLTAGE], gate, row[SD_CURRENT])
                for (row in fb.backward) bwd.addData(row[SET_SD_VOLTAGE], gate, row[SD_CURRENT])

            }

            val maxLinMobility = LinkedHashMap<Double, Double>()
            val maxSatMobility = LinkedHashMap<Double, Double>()

            for ((drain, data) in fwd.split(SDV)) {

                if (data.numRows < 2) continue

                val vG      = data.getColumns(SGV)
                val iD      = data.getColumns(SDI)
                val linGrad = Interpolation.interpolate1D(vG, iD.map { x -> abs(x) }).derivative()
                val satGrad = Interpolation.interpolate1D(vG, iD.map { x -> sqrt(abs(x)) }).derivative()

                for (gate in data.getUniqueValues(SGV).sorted()) {

                    val linMobility = 1e4 * abs((length / (capacitance * width)) * (linGrad.value(gate) / drain))
                    val satMobility = 1e4 * 2.0 * satGrad.value(gate).pow(2) * length / (width * capacitance)
                    val params      = ArrayList(parameters)
                    params         += Gate(gate, 0.0)
                    params         += Drain(drain, 0.0)

                    if (linMobility.isFinite()) {
                        quantities += FwdLinMobility(
                            linMobility,
                            0.0,
                            params,
                            possibleParameters
                        )
                        maxLinMobility[gate] = max(maxLinMobility[gate] ?: 0.0, linMobility)
                    }
                    if (satMobility.isFinite()) {
                        quantities += FwdSatMobility(
                            satMobility,
                            0.0,
                            params,
                            possibleParameters
                        )
                        maxSatMobility[gate] = max(maxSatMobility[gate] ?: 0.0, satMobility)
                    }

                }

            }

            for ((drain, data) in bwd.split(SDV)) {

                if (data.numRows < 2) continue

                val vG      = data.getColumns(SGV)
                val iD      = data.getColumns(SDI)
                val linGrad = Interpolation.interpolate1D(vG, iD.map { x -> abs(x) }).derivative()
                val satGrad = Interpolation.interpolate1D(vG, iD.map { x -> sqrt(abs(x)) }).derivative()

                for (gate in data.getUniqueValues(SGV).sorted()) {

                    val linMobility = 1e4 * abs((length / (capacitance * width)) * (linGrad.value(gate) / drain))
                    val satMobility = 1e4 * 2.0 * satGrad.value(gate).pow(2) * length / (width * capacitance)
                    val params      = ArrayList(parameters)
                    params         += Gate(gate, 0.0)
                    params         += Drain(drain, 0.0)

                    if (linMobility.isFinite()) {
                        quantities += BwdLinMobility(
                            linMobility,
                            0.0,
                            params,
                            possibleParameters
                        )
                        maxLinMobility[gate] = max(maxLinMobility[gate] ?: 0.0, linMobility)
                    }
                    if (satMobility.isFinite()) {
                        quantities += BwdSatMobility(
                            satMobility,
                            0.0,
                            params,
                            possibleParameters
                        )
                        maxSatMobility[gate] = max(maxSatMobility[gate] ?: 0.0, satMobility)
                    }

                }

            }

            for ((gate, max) in maxLinMobility) {
                quantities += MaxLinMobility(max, 0.0, ArrayList(parameters).apply { add(Gate(gate, 0.0)) })
            }

            for ((gate, max) in maxSatMobility) {
                quantities += MaxSatMobility(max, 0.0, ArrayList(parameters).apply { add(Gate(gate, 0.0)) })
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> = emptyList()

}