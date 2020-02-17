package org.oefet.fetch.analysis

import jisa.experiment.ResultTable
import java.util.*

interface Curve {


    val fwdMob: ResultTable

    val bwdMob: ResultTable

    val data: ResultTable

    val name: String

    val length: Double

    val width: Double

    val channelThickness: Double

    val dielectricThickness: Double

    val fppSeparation: Double

    val permittivity: Double

    val temperature: Double

    val variables: Map<String, Double>

    val variableString: String get() {

        val parts = LinkedList<String>()

        for ((name, value) in data.attributes) {
            if (name !in NON_USER_VARIABLES) {
                parts += "$name = $value"
            }
        }

        return parts.joinToString(", ")

    }

    companion object {

        val NON_USER_VARIABLES = arrayOf(
            "type",
            "length",
            "width",
            "channelThickness",
            "fppSeparation",
            "dielectricThickness",
            "dielectricPermittivity",
            "name"
        )

    }

}