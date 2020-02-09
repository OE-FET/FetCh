package org.oefet.fetch.gui

import jisa.enums.Icon
import jisa.experiment.ActionQueue
import jisa.gui.Fields
import jisa.gui.Grid
import jisa.maths.Range

object Temperature : Grid("Temperature", 2) {

    val basic = Fields("Temperature Set-Points")

    val name = basic.addTextField("Sweep Name")

    init {
        basic.addSeparator()
    }

    val minT = basic.addDoubleField("Start [K]", 300.0)
    val maxT = basic.addDoubleField("Stop [K]", 50.0)
    val numT = basic.addIntegerField("No. Steps", 6)

    init {
        basic.addSeparator()
    }

    val stabPerc = basic.addDoubleField("Stability Range [%]", 1.0)
    val stabTime = basic.addDoubleField("Stability Time [s]", 600.0)

    val subQueue = ActionQueue()
    val names = ArrayList<String>()
    val queueList = FetChQueue("Sweep Measurements", subQueue)

    init {

        setGrowth(true, false)
        addAll(basic, queueList)
        setIcon(Icon.SNOWFLAKE)
        basic.loadFromConfig("temp-basic", Settings)

        queueList.addTSweep.isDisabled = true
        queueList.addTChange.isDisabled = true

    }

    fun disable(flag: Boolean) {
        basic.setFieldsDisabled(flag)
    }

    fun askForSweep(queue: ActionQueue) {

        name.set("TSweep${queue.size}")

        if (showAndWait()) {

            for (T in Range.linear(minT.get(), maxT.get(), numT.get())) {

                queue.addAction("Change temperature to $T K") {

                    val tc = Configuration.tControl.get() ?: throw Exception("No temperature controller configured")

                    tc.targetTemperature = T
                    tc.useAutoHeater()
                    tc.waitForStableTemperature(T, stabPerc.get(), (stabTime.get() * 1000.0).toLong())

                }

                for (action in subQueue) {

                    val copy = action.copy()

                    if (action is ActionQueue.MeasureAction && copy is ActionQueue.MeasureAction) {

                        if (action.resultPath != null) copy.setResultsPath(
                            action.resultPath.replace(
                                Measure.baseFile,
                                "${Measure.baseFile}-${name.get()}=${T}K"
                            )
                        )

                        copy.setBefore { Measure.showMeasurement(copy); Results.addMeasurement(copy); }
                        copy.name = "${copy.name} (${name.get()} = $T K)"
                    }

                    queue.addAction(copy)
                }

            }

        }

    }

    fun askForSingle(queue: ActionQueue) {

        if (showAndWait()) {

            for (T in Range.linear(minT.get(), maxT.get(), numT.get())) {

                queue.addAction("Change Temperature to $T K") {

                    val tc = Configuration.tControl.get() ?: throw Exception("No temperature controller configured")

                    tc.targetTemperature = T
                    tc.useAutoHeater()
                    tc.waitForStableTemperature(T, stabPerc.get(), (stabTime.get() * 1000.0).toLong())

                }


            }

        }

    }

    override fun showAndWait(): Boolean {
        subQueue.clear()
        names.clear()
        return super.showAndWait()
    }

    val values: Range<Double>
        get() = Range.linear(minT.get(), maxT.get(), numT.get())

    val stabilityPercentage: Double
        get() = stabPerc.get()

    val stabilityTime: Long
        get() = (stabTime.get() * 1000.0).toLong()

}