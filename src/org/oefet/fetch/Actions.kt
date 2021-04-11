package org.oefet.fetch

import org.oefet.fetch.action.FAction
import org.reflections.Reflections
import kotlin.reflect.full.primaryConstructor

object Actions {

    val types = Reflections("org.oefet.fetch.action")
                    .getSubTypesOf(FAction::class.java)
                    .map { Config(it.getConstructor().newInstance()) }
                    .sortedBy { it.name }

    class Config(private val example: FAction) {

        val name = example.name

        fun create(): FAction {
            return example::class.primaryConstructor!!.call()
        }

    }

}