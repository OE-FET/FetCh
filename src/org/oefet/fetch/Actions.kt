package org.oefet.fetch

import org.oefet.fetch.action.Action
import org.reflections.Reflections
import kotlin.reflect.full.primaryConstructor

object Actions {

    val types = Reflections("org.oefet.fetch.action")
                    .getSubTypesOf(Action::class.java)
                    .map { Config(it.getConstructor().newInstance()) }
                    .sortedBy { it.name }

    class Config(private val example: Action) {

        val name = example.name

        fun create(): Action {
            return example::class.primaryConstructor!!.call()
        }

    }

}