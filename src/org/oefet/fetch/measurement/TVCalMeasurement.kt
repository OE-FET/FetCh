package org.oefet.fetch.measurement

import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import java.util.*

class TVCalMeasurement : FetChMeasurement() {

    override val type = "TV Calibration"

    var ground: SMU?    = null
    var tMeter: TMeter? = null
    var heater: SMU?    = null
    var strip: SMU?     = null

    private fun loadInstruments() {

        heater  = Instruments.htSMU
        ground  = Instruments.gdSMU
        strip   = Instruments.sdSMU
        tMeter  = Instruments.tMeter

        val errors = LinkedList<String>()

        if (heater == null) {
            errors += "Heater is not configured"
        }

        if (strip == null) {
            errors += "Source-drain channel is not configured"
        }

        if (tMeter == null) {
            errors += "No gate channel configured."
        }

        if (errors.isNotEmpty()) {
            throw Exception(errors.joinToString(", "))
        }

    }

    override fun run(results: ResultTable) {

        loadInstruments()

        val tMeter = this.tMeter!!
        val heater = this.heater!!
        val strip  = this.strip!!



    }

    override fun onFinish() {
        TODO("Not yet implemented")
    }

    override fun getLabel(): String {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun getColumns(): Array<Col> {
        TODO("Not yet implemented")
    }

    override fun setLabel(value: String?) {
        TODO("Not yet implemented")
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

}