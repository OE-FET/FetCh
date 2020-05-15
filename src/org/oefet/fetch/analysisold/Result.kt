package org.oefet.fetch.analysisold

import org.oefet.fetch.EPSILON

interface Result {

    val temperature:         Double?
    val repeatCount:         Int?
    val stressTime:          Double?
    val number:              Int?
    val length:              Double
    val width:               Double
    val thick:               Double
    val fppSeparation:       Double
    val dielectricThickness: Double
    val dielectricConstant:  Double
    val mobility:            Double?
    val conductivity:        Double?
    val capacitance: Double get() = dielectricConstant * EPSILON / dielectricThickness

}