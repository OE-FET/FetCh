package org.oefet.fetch.action

import jisa.experiment.Col
import org.oefet.fetch.FetChEntity

abstract class FetChAction(private val name: String) : FetChEntity() {


    override fun getName(): String {
        return this.name
    }

    override fun getColumns(): Array<Col> {
        return emptyArray()
    }

    override fun setLabel(value: String) {
    }

    override fun onFinish() {

    }

    override fun onError() {

    }

    override fun onInterrupt() {

    }

}