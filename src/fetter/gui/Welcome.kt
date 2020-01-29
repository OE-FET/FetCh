package fetter.gui

import jisa.enums.Icon
import jisa.gui.Colour
import jisa.gui.Doc
import jisa.gui.Grid

class Welcome : Grid("Welcome", 1) {

    val doc = Doc("Welcome")

    init {

        setIcon(Icon.LIGHTBULB)

        add(doc)

        doc.addImage(Welcome::class.java.getResource("fEt.png"))
            .setAlignment(Doc.Align.CENTRE)
        doc.addHeading("FETTER")
            .setAlignment(Doc.Align.CENTRE)
        doc.addText("William Wood 2020")
            .setAlignment(Doc.Align.CENTRE)
        doc.addText("Welcome to the OE-FET transistor testing suite, FETTER. This software allows you to:")
        doc.addList(false)
            .addItem("Measure output curves")
            .addItem("Measure transfer curves")
            .addItem("Control temperature to measure temperature-dependent output/transfer curves")

        doc.addText("")
        doc.addText("Enable and configure all the individual measurements you want to perform in their respective sections, then initiate them in the \"Measurement\" tab.")
        doc.addText("")
        doc.addText("Below you will find information on what each tab does and how to use them.")
        doc.addText("")

        doc.addSubHeading("Connections")
        doc.addText("You shouldn't need to change anything in this tab once the program has been set-up.")
            .setColour(Colour.RED)
        doc.addText("The connections tab is where you configure the connections to the instruments you are using to perform FET measurements. "
            + "There are a number of possible \"slots\" to connect various types of instruments, they don't all need to be connected. "
            + "Red means the connection is not established, yellow means a connection is in progress and green means successfully established.")
        doc.addText("When starting this program, all previously activated connections from the last run will automatically attempt to re-establish. "
            + "Ideally, you should never need to change anything here once the program has been initially set-up.")

        doc.addSubHeading("Configuration")
        doc.addText("You shouldn't need to change anything in this tab once the program has been set-up.")
            .setColour(Colour.RED)
        doc.addText("The configuration tab lets you choose which connected instruments, and which channels in those instruments, are used for each experimental parameter. "
            + "These include:")
        doc.addList(false)
            .addItem("Grounded Channel (when using an SPA)")
            .addItem("Source-Drain Channel - (required) The channel/instrument used for forcing voltages between source and drain and measuring drain current")
            .addItem("Source-Gate Channel - (required) The channel/instrument used for forcing voltage between source and gate and measuring leakage current")
            .addItem("2x Voltage Probe Channels - (optional) Used for performing four-point-probe voltage measurements")
            .addItem("Temperature Control - (optional) Used for controlling temperature values if in a temperature controlled system")
            .addItem("Thermometer - (optional) Used for measuring the temperature of the device during a sweep (can be the same as the temperature control)")

        doc.addSubHeading("Temperature")
        doc.addText("The temperature tab lets you configure the temperature steps and stability criteria for temperature-depended measurements.")
        doc.addText("If you are not wanting to perform temperature-dependent measurements or the set-up does not have a temperature controller then this tab should be disabled by un-ticking the \"Enabled\" setting.")
            .setColour(Colour.RED)

        doc.addSubHeading("Output Curve and Transfer Curve")
        doc.addText("The output/transfer curve tabs let you configure the measurement and voltage parameters for output and transfer curve measurements respectively.")
        doc.addText("The measurement parameters you can configure are:")
        doc.addList(false)
            .addItem("Enabled - Should this measurement be run?")
            .addItem("Integration Time - The integration time to use, in seconds, for each measurement")
            .addItem("Delay Time - The amount of time to wait after applying all voltages before taking the measurement (in seconds)")
        doc.addText("To configure the source-drain and source-gate voltages used you can specify:")
        doc.addList(false)
            .addItem("Start - The first voltage in the voltage sweep (in Volts)")
            .addItem("Stop - The last voltage in the voltage sweep (in Volts)")
            .addItem("No. Steps - The number of steps (inclusive) to take sweeping from \"start\" to \"stop\"")
            .addItem("Sweep both ways - Whether this parameters should do a second sweep in the reverse direction after completing the first")

        doc.addSubHeading("Measurement")
        doc.addText("The measurement tab is where you specify the output directory and name as well as information about the device you are measuring. "
            + "You can then initiate the measurement by pressing \"Start\" to perform all enabled measurements in sequence.")
        doc.addText("When a measurement is running, live results will also be displayed here.")
        doc.addText("All recorded measurements are immediately written to files in the selected output directory as they are taken.")
        doc.addText("You can also save the plots generated in this section by pressing \"Save Plots\".")

    }

}