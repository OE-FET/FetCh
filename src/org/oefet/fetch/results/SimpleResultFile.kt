package org.oefet.fetch.results

import javafx.scene.image.Image
import jisa.enums.Icon
import jisa.experiment.ResultTable
import jisa.gui.Plot
import org.oefet.fetch.quantities.Quantity

class SimpleResultFile(name: String, tag: String, data: ResultTable, extra: List<Quantity>): FetChResult(name, tag, Icon.DATA.blackImage, data, extra) {

    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> {
        return emptyList()
    }


}