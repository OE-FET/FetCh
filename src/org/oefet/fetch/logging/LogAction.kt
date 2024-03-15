package org.oefet.fetch.logging

import jisa.results.Column

interface LogAction {

    val title:  String
    val yLabel: String
    val yUnits: String
    val column: Column<Double>
    var isEnabled: Boolean
    val value: Double

}