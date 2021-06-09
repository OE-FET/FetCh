package org.oefet.fetch.bundle

import jisa.experiment.Col
import jisa.experiment.ResultTable
import jisa.experiment.queue.ActionQueue
import org.oefet.fetch.FetChEntity

class FetChBundle(private val name: String) : FetChEntity() {

    val queue = ActionQueue()

    override fun getName(): String {
        return this.name;
    }

    override fun run(results: ResultTable?) {
        TODO("Not yet implemented")
    }

    override fun onFinish() {
        TODO("Not yet implemented")
    }

    override fun getColumns(): Array<Col> {
        TODO("Not yet implemented")
    }

}