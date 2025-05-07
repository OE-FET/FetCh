package org.oefet.fetch.gui.elements

import jisa.enums.Icon
import jisa.gui.Grid
import jisa.gui.ListDisplay
import jisa.gui.Table
import jisa.gui.Tabs
import org.oefet.fetch.logging.Dash
import org.oefet.fetch.logging.DashValue

class DashDisplay(val dash: Dash) : Tabs(dash.name) {

    val plotGrid  = Grid("Plots")
    val dataTable = Table("Data Table")
    val actions   = ListDisplay<DashValue>("Logged Values")

    init {

        setIcon(Icon.DASHBOARD)

    }



}