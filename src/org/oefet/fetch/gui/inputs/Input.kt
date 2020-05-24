package org.oefet.fetch.gui.inputs

import jisa.experiment.ActionQueue
import org.reflections.Reflections

interface Input {

    fun getTitle(): String

    fun ask(queue: ActionQueue)

    class Type(private val input: Input) {

        val name: String = input.getTitle()

        fun create(): Input = input.javaClass.getConstructor().newInstance()

    }

}

interface ActionInput : Input {

    companion object {

        val types = Reflections("org.oefet.fetch.gui.inputs").getSubTypesOf(ActionInput::class.java).map {
            Input.Type(it.getConstructor().newInstance())
        }

    }

}

interface SweepInput: Input {

    companion object {

        val types  = Reflections("org.oefet.fetch.gui.inputs").getSubTypesOf(SweepInput::class.java).map {
            Input.Type(it.getConstructor().newInstance())
        }

    }

}