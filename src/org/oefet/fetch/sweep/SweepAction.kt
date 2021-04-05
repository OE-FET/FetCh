package org.oefet.fetch.sweep

import jisa.experiment.ActionQueue
import jisa.gui.Grid
import jisa.gui.MeasurementConfigurator

class SweepAction(name: String, val grid: Grid, val config: MeasurementConfigurator, val measurement: Sweep) : ActionQueue.MultiAction(name) {


}