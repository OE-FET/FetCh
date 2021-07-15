package org.oefet.fetch.results

import jisa.enums.Icon
import jisa.results.ResultTable
import org.oefet.fetch.quantities.Quantity

class SimpleResultFile(name: String, tag: String, data: ResultTable, extra: List<Quantity>): FetChResult(name, tag, Icon.DATA.blackImage, data, extra) {

    override fun calculateHybrids(otherQuantities: List<Quantity>): List<Quantity> {
        return emptyList()
    }


}