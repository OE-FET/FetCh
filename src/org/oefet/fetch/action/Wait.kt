package org.oefet.fetch.action

import jisa.Util
import jisa.control.RTask
import jisa.enums.Icon
import jisa.gui.Progress
import jisa.results.ResultTable

class Wait : FetChAction("Wait", Icon.CLOCK.blackImage) {

    var task: RTask? = null

    val hours   by userInput("Time", "Hours", 0)
    val minutes by userInput("Time", "Minutes", 0)
    val seconds by userInput("Time", "Seconds", 0)
    val millis  by userInput("Time", "Milliseconds", 0)
    val time get() = millis + (seconds * 1000) + (minutes * 60 * 1000) + (hours * 60 * 60 * 1000)
    val progress = Progress("")

    override fun createDisplay(data: ResultTable): Progress {
        progress.title = "Wait for ${Util.msToString(time.toLong())}"
        return progress
    }

    override fun run(results: ResultTable) {

        progress.setProgress(0, time)

        task = RTask(50) { t ->
            progress.setProgress(t.mSecFromStart, time)
            progress.status = "${Util.msToPaddedString(time.toLong() - t.mSecFromStart)} remaining..."
        }

        task?.start()

        sleep(time)

    }

    override fun onFinish() {
        task?.stop()
    }

    override fun getLabel(): String {
        return Util.msToString(time.toLong())
    }

}