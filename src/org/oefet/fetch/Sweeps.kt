package org.oefet.fetch

import org.oefet.fetch.action.Action
import org.oefet.fetch.sweep.Sweep
import org.reflections.Reflections
import kotlin.reflect.full.primaryConstructor

object Sweeps {

    val types = Reflections("org.oefet.fetch.sweep")
                    .getSubTypesOf(Sweep::class.java)
                    .map { Config(it.getConstructor().newInstance()) }
                    .sortedBy { it.name }

    class Config(private val example: Sweep) {

        val name = example.name

        fun create(): Sweep {
            return example::class.primaryConstructor!!.call()
        }

    }

}