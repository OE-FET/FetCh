package fetter.measurement

import fetter.gui.Configuration
import jisa.Util
import jisa.devices.SMU
import jisa.devices.TMeter
import jisa.devices.VMeter
import jisa.experiment.Col
import jisa.experiment.Measurement
import jisa.experiment.ResultTable
import jisa.maths.Range

class OutputMeasurement : Measurement() {

    private var sdSMU : SMU? = null
    private var sgSMU : SMU? = null
    private var gdSMU : SMU? = null
    private var fpp1  : VMeter? = null
    private var fpp2  : VMeter? = null
    private var tm    : TMeter? = null

    private var minVSD = 0.0
    private var maxVSD = 60.0
    private var numVSD = 7
    private var symVSD = true

    private var minVSG = 0.0
    private var maxVSG = 60.0
    private var numVSG = 61
    private var symVSG = true

    private var intTime = 1.0 / 50.0
    private var delTime = 500

    fun configureSD(start: Double, stop: Double, steps: Int, sym: Boolean): OutputMeasurement {

        minVSD = start
        maxVSD = stop
        numVSD = steps
        symVSD = sym

        return this

    }

    fun configureSG(start: Double, stop: Double, steps: Int, sym: Boolean): OutputMeasurement {

        minVSG = start
        maxVSG = stop
        numVSG = steps
        symVSG = sym

        return this

    }

    fun configureTimes(integration: Double, delay: Double): OutputMeasurement {

        intTime = integration
        delTime = (delay * 1000).toInt()

        return this

    }

    fun loadInstruments() {

        val instruments = Configuration.getInstruments()

        if (!instruments.hasSD || !instruments.hasSG) {
            throw Exception("Source-Drain and Source-Gate SMUs must be configured first")
        }

        sdSMU = instruments.sdSMU!!
        sgSMU = instruments.sgSMU!!
        gdSMU = instruments.gdSMU
        fpp1  = instruments.fpp1
        fpp2  = instruments.fpp2
        tm    = instruments.tm

    }

    override fun run(results: ResultTable) {

        loadInstruments()

        val sdSMU = this.sdSMU!!
        val sgSMU = this.sgSMU!!

        var sdVoltages = Range.linear(minVSD, maxVSD, numVSD)
        var sgVoltages = Range.linear(minVSG, maxVSG, numVSG)

        if (symVSD) sdVoltages = sdVoltages.mirror()
        if (symVSG) sgVoltages = sgVoltages.mirror()

        sdSMU.turnOff()
        sgSMU.turnOff()
        gdSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

        // Configure initial source modes
        sdSMU.voltage = minVSD
        sgSMU.voltage = minVSG
        gdSMU?.voltage = 0.0

        // Configure integration times
        sdSMU.integrationTime = intTime
        sgSMU.integrationTime = intTime
        fpp1?.integrationTime = intTime
        fpp2?.integrationTime = intTime

        sdSMU.turnOn()
        sgSMU.turnOn()
        gdSMU?.turnOn()
        fpp1?.turnOn()
        fpp2?.turnOn()

        for (vSG in sgVoltages) {

            sgSMU.voltage = vSG

            for (vSD in sdVoltages) {

                sdSMU.voltage = vSD

                sleep(delTime)

                results.addData(
                    vSD, vSG,
                    sdSMU.voltage, sdSMU.current,
                    sgSMU.voltage, sgSMU.current,
                    fpp1?.voltage ?: 0.0, fpp2?.voltage ?: 0.0,
                    tm?.temperature ?: 0.0
                )

            }

        }

    }

    override fun onFinish() {

        sdSMU?.turnOff()
        sgSMU?.turnOff()
        gdSMU?.turnOff()
        fpp1?.turnOff()
        fpp2?.turnOff()

    }

    override fun getName(): String = "Output Measurement"

    override fun getColumns(): Array<Col> {

        return arrayOf(
            Col("Set SD Voltage", "V"),
            Col("Set SG Voltage", "V"),
            Col("SD Voltage", "V"),
            Col("SD Current", "A"),
            Col("SG Voltage", "V"),
            Col("SG Current", "A"),
            Col("Four Point Probe 1", "V"),
            Col("Four Point Probe 2", "V"),
            Col("Temperature", "K")
        )

    }

    override fun onInterrupt() {

        Util.errLog.println("Transfer measurement interrupted.")

    }

}