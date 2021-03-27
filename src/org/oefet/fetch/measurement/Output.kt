package org.oefet.fetch.measurement

import jisa.Util
import jisa.devices.interfaces.SMU
import jisa.devices.interfaces.TMeter
import jisa.devices.interfaces.VMeter
import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.maths.Range
import org.oefet.fetch.gui.elements.OutputPlot
import org.oefet.fetch.quantities.Quantity
import org.oefet.fetch.results.OutputResult

class Output : FMeasurement("Output Measurement", "Output", "Output") {

    // Parameters
    var delTime    = 500
    var sdVoltages = Range.step(0, 60, 1)
    var symVSD     = true
    var sgVoltages = Range.step(0, 60, 10)

    // Required instruments
    lateinit var sd: SMU
    lateinit var sg: SMU

    // Optional instruments
    var gd:  SMU?    = null
    var fp1: VMeter? = null
    var fp2: VMeter? = null
    var tM:  TMeter? = null

    init {
        
        addParameter("Basic", "Delay Time [s]", 0.5)            { delTime = (1000.0 * it).toInt() }
        addParameter("Source-Drain", "Voltage [V]", sdVoltages) { sdVoltages = it }
        addParameter("Source-Drain", "Sweep Both Ways", symVSD) { symVSD = it }
        addParameter("Source-Gate", "Voltage [V]", sgVoltages)  { sgVoltages = it }

        addOptionalInstrument("Ground Channel (SPA)", SMU::class)          { gd  = it }
        addRequiredInstrument("Source-Drain Channel", SMU::class)          { sd  = it }
        addRequiredInstrument("Source-Gate Channel", SMU::class)           { sg  = it }
        addOptionalInstrument("Four-Point-Probe Channel 1", VMeter::class) { fp1 = it }
        addOptionalInstrument("Four-Point-Probe Channel 2", VMeter::class) { fp2 = it }

    }

    companion object {
        val SET_SD_VOLTAGE = Col("Set SD Voltage", "V")
        val SET_SG_VOLTAGE = Col("Set SG Voltage", "V")
        val SD_VOLTAGE     = Col("SD Voltage", "V")
        val SD_CURRENT     = Col("SD Current", "A")
        val SG_VOLTAGE     = Col("SG Voltage", "V")
        val SG_CURRENT     = Col("SG Current", "A")
        val FPP_1          = Col("Four Point Probe 1", "V")
        val FPP_2          = Col("Four Point Probe 2", "V")
        val TEMPERATURE    = Col("Temperature", "K")
        val GROUND_CURRENT = Col("Ground Current", "A")
    }

    override fun createPlot(data: ResultTable): OutputPlot {
        return OutputPlot(data)
    }

    override fun processResults(data: ResultTable, extra: List<Quantity>): OutputResult {
        return OutputResult(data, extra)
    }

    override fun getColumns(): Array<Col> {

        return arrayOf(
            SET_SD_VOLTAGE,
            SET_SG_VOLTAGE,
            SD_VOLTAGE,
            SD_CURRENT,
            SG_VOLTAGE,
            SG_CURRENT,
            FPP_1,
            FPP_2,
            TEMPERATURE,
            GROUND_CURRENT
        )

    }

    override fun run(results: ResultTable) {

        results.setAttribute("Integration Time", "${sd.integrationTime} s")
        results.setAttribute("Delay Time", "$delTime ms")

        sd.turnOff()
        sg.turnOff()
        gd?.turnOff()
        fp1?.turnOff()
        fp2?.turnOff()

        // Configure initial source modes
        sd.voltage = sdVoltages.first()
        sg.voltage = sgVoltages.first()
        gd?.voltage = 0.0

        sd.turnOn()
        sg.turnOn()
        gd?.turnOn()
        fp1?.turnOn()
        fp2?.turnOn()

        for (vSG in sgVoltages) {

            sg.voltage = vSG

            for (vSD in (if (symVSD) sdVoltages.mirror() else sdVoltages)) {

                sd.voltage = vSD

                sleep(delTime)

                results.addData(
                    vSD, vSG,
                    sd.voltage, sd.current,
                    sg.voltage, sg.current,
                    fp1?.voltage ?: Double.NaN, fp2?.voltage ?: Double.NaN,
                    tM?.temperature ?: Double.NaN,
                    gd?.current ?: Double.NaN
                )

            }

        }

    }

    override fun onFinish() {

        runRegardless { sd.turnOff() }
        runRegardless { sg.turnOff() }
        runRegardless { gd?.turnOff() }
        runRegardless { fp1?.turnOff() }
        runRegardless { fp2?.turnOff() }

    }

    override fun onInterrupt() {

        Util.errLog.println("Transfer measurement interrupted.")

    }

    override fun onError() {

    }

}
