import jisa.Util
import jisa.enums.Icon
import jisa.experiment.Measurement
import jisa.gui.Fields
import jisa.gui.GUI
import jisa.gui.Grid
import java.lang.Exception
import java.nio.file.Paths

class Measure(val mainWindow: MainWindow) : Grid("Measurement", 1) {

    val basic = Fields("Data Output Settings")
    val name = basic.addTextField("Name")
    val dir = basic.addDirectorySelect("Output Directory")
    val start = basic.addButton("Start Measurement", this::runMeasurement)

    init {
        setGrowth(true, false)
        setIcon(Icon.FLASK)
        add(basic)
    }

    fun runMeasurement() {

        disable(true)

        try {

            val fileName = name.get()
            val fileDir = dir.get()
            val path = Paths.get(fileDir, fileName).toString()

            // Get which measurements to do
            val doTemperature = mainWindow.temperature.enabled.get()
            val doOutput = mainWindow.output.enabled.get()
            val doTransfer = mainWindow.transfer.enabled.get()

            // Get the configured instruments
            val sdSMU = mainWindow.configuration.sourceDrain.get()
            val sgSMU = mainWindow.configuration.sourceGate.get()
            val fpp1 = mainWindow.configuration.fourPP1.get()
            val fpp2 = mainWindow.configuration.fourPP2.get()
            val tc = mainWindow.configuration.tControl.get()
            val tm = mainWindow.configuration.tMeter.get()

            // If we are to use temperature set-points we need a temperature controller
            if (doTemperature && tc == null) {
                throw Exception("Temperature dependent measurements require a temperature controller.")
            }

            val measurements = HashMap<String, Measurement>()

            if (doOutput) {

                val output = OutputMeasurement(sdSMU, sgSMU, fpp1, fpp2, tm)

                output.configureSD(
                    mainWindow.output.minSDV.get(),
                    mainWindow.output.maxSDV.get(),
                    mainWindow.output.numSDV.get(),
                    mainWindow.output.symSDV.get()
                ).configureSG(
                    mainWindow.output.minSGV.get(),
                    mainWindow.output.maxSGV.get(),
                    mainWindow.output.numSGV.get(),
                    mainWindow.output.symSGV.get()
                ).configureTimes(
                    mainWindow.output.intTime.get(),
                    mainWindow.output.delTime.get()
                )

                measurements["output"] = output

            }

            if (doTransfer) {

                val transfer = TransferMeasurement(sdSMU, sgSMU, fpp1, fpp2, tm)

                transfer.configureSD(
                    mainWindow.transfer.minSDV.get(),
                    mainWindow.transfer.maxSDV.get(),
                    mainWindow.transfer.numSDV.get(),
                    mainWindow.transfer.symSDV.get()
                ).configureSG(
                    mainWindow.transfer.minSGV.get(),
                    mainWindow.transfer.maxSGV.get(),
                    mainWindow.transfer.numSGV.get(),
                    mainWindow.transfer.symSGV.get()
                ).configureTimes(
                    mainWindow.transfer.intTime.get(),
                    mainWindow.transfer.delTime.get()
                )

                measurements["transfer"] = transfer

            }

            if (doTemperature) {

                val temperatures = Util.makeLinearArray(
                    mainWindow.temperature.minT.get(),
                    mainWindow.temperature.maxT.get(),
                    mainWindow.temperature.numT.get()
                )

                val stabPerc = mainWindow.temperature.stabPerc.get()
                val stabTime = (mainWindow.temperature.stabTime.get() * 1000).toLong()

                for (T in temperatures) {

                    tc.targetTemperature = T
                    tc.waitForStableTemperature(T, stabPerc, stabTime)

                    for ((name, measurement) in measurements) {

                        measurement.newResults("%s-%sK-%s.csv".format(path, T, name))
                        measurement.run()

                        if (measurement.wasStopped()) {
                            throw InterruptedException("Measurement Stopped");
                        }

                    }

                }

            } else {

                for ((name, measurement) in measurements) {

                    measurement.newResults("%s-%s.csv".format(path, name))
                    measurement.run()

                    if (measurement.wasStopped()) {
                        throw InterruptedException("Measurement Stopped");
                    }

                }

            }

            GUI.infoAlert("Measurement Complete", "The measurement completed without error.")

        } catch (e: InterruptedException) {
            GUI.warningAlert("Measurement Stopped", "The measurement was stopped before completion.")
        } catch (e: Exception) {
            GUI.errorAlert("Measurement Error", "Measurement Error", e.message, 600.0)
        } finally {
            disable(false)
        }

    }

    fun disable(flag: Boolean) {
        start.isDisabled = flag
        basic.setFieldsDisabled(flag)
        mainWindow.temperature.disable(flag)
        mainWindow.output.disable(flag)
        mainWindow.transfer.disable(flag)
    }

}