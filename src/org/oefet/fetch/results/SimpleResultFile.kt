package org.oefet.fetch.results

import javafx.scene.image.Image
import jisa.enums.Icon
import jisa.experiment.ResultTable
import jisa.gui.Plot
import org.oefet.fetch.quantities.Quantity

class SimpleResultFile(name: String, override val data: ResultTable, extra: List<Quantity>): ResultFile {

    override val parameters    = extra.toMutableList()
    override val quantities    = emptyList<Quantity>().toMutableList()
    override val plot: Plot?   = null
    override val name: String  = "$name (${data.getAttribute("Name") ?: "Unknown Name"})"
    override val image: Image  = Icon.DATA.blackImage
    override val label: String = data.getAttribute("Type") ?: ""

    override var length: Double       = 0.0
    override var separation: Double   = 0.0
    override var width: Double        = 0.0
    override var thickness: Double    = 0.0
    override var dielectric: Double   = 0.0
    override var permittivity: Double = 0.0
    override var temperature: Double  = Double.NaN
    override var repeat: Double       = 0.0
    override var stress: Double       = 0.0
    override var field: Double        = 0.0

    init {
        parseParameters(data, extra, Double.NaN)
    }

    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> {
        return emptyList()
    }


}