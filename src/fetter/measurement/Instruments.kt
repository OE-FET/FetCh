package fetter.measurement

import jisa.devices.SMU
import jisa.devices.TC
import jisa.devices.TMeter
import jisa.devices.VMeter

class Instruments(
    val sdSMU: SMU?,
    val sgSMU: SMU?,
    val gdSMU: SMU?,
    val fpp1: VMeter?,
    val fpp2: VMeter?,
    val tc: TC?,
    val tm: TMeter?
) {


    val hasSD: Boolean
        get() {
            return sdSMU != null
        }

    val hasSG: Boolean
        get() {
            return sdSMU != null
        }

    val hasTC: Boolean
        get() {
            return tc != null
        }

    val hasTM: Boolean
        get() {
            return tm != null
        }

}