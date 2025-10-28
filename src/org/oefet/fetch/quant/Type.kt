package org.oefet.fetch.quant

enum class Type(val units: String) {

    VOLTAGE("V"),
    CURRENT("A"),
    TEMPERATURE("K"),
    DISTANCE("m"),
    CONDUCTANCE("S"),
    CONDUCTIVITY("S/cm"),
    RESISTANCE("Ω"),
    RESISTIVITY("Ω cm"),
    FREQUENCY("Hz"),
    MOBILITY("cm^2/Vs"),
    E_FIELD("V/m"),
    B_FIELD("T"),
    CHARGE("C"),
    CHARGE_DENSITY("C/m^3"),
    INDEX(""),
    LABEL(""),
    SEEBECK_COEFFICIENT("V/K"),
    SEEBECK_POWER_COEFFICIENT("V/W"),
    HALL_COEFFICIENT("m^3/C"),
    LINEAR_SPEED("m/s"),
    ROTATIONAL_SPEED("rad/s"),
    TIME("s"),
    RELATIVE_PERMITTIVITY(""),
    POWER("W"),
    POWER_DENSITY_3D("W/m^3"),
    POWER_DENSITY_2D("W/m^2"),
    CARRIER_DENSITY("cm^-3"),
    UNKNOWN(""),
    PHASE("rad"),
    COUNT(""),
    WAVELENGTH("m")

}