package org.oefet.fetch.gui.inputs

import jisa.control.SRunnable
import jisa.experiment.ActionQueue

class InputAction(name: String, val input: Input, runnable: SRunnable) : ActionQueue.Action(name, runnable) {



}