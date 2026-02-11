package org.oefet.fetch.action

import jisa.results.ResultTable
import org.oefet.fetch.Entity

abstract class Action(name: String) : Entity(name) {

    override fun after(data: ResultTable?) {

    }

    override fun interrupted(data: ResultTable?) {

    }

    override fun error(data: ResultTable?, exception: List<Throwable?>?) {

    }

}