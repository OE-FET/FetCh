package org.oefet.fetch

import jisa.experiment.ResultTable
import org.oefet.fetch.analysis.quantities.Quantity
import org.oefet.fetch.analysis.results.*
import org.oefet.fetch.measurement.*

object Measurements {

    class Config(val type: String, val measurement: () -> FetChMeasurement, val result: (ResultTable, List<Quantity>) -> ResultFile) {

        val name = measurement().name

        fun createMeasurement(): FetChMeasurement = measurement()
        fun createResult(data: ResultTable, extra: List<Quantity> = emptyList()): ResultFile = result(data, extra)


    }

    val types = listOf(
        Config("Output",           ::OutputMeasurement,   ::OutputResult),
        Config("Transfer",         ::TransferMeasurement, ::TransferResult),
        Config("Sync",             ::SyncMeasurement,     ::TransferResult),
        Config("FPP Conductivity", ::FPPMeasurement,      ::CondResult),
        Config("AC Hall",          ::ACHallMeasurement,   ::ACHallResult),
        Config("Thermal Voltage",  ::TVMeasurement,       ::TVResult)
    )

    fun loadResultFile(data: ResultTable, extra: List<Quantity> = emptyList()): ResultFile? {

        return try {
            types.find { it.type == data.getAttribute("Type") }?.createResult(data, extra)
        } catch (e: Exception) {
            null
        }

    }

}