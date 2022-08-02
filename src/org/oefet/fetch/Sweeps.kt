package org.oefet.fetch

import org.oefet.fetch.sweep.FetChSweep
import org.reflections.Reflections
import kotlin.reflect.full.primaryConstructor

object Sweeps {

    val types = Reflections("org.oefet.fetch.sweep")
                    .getSubTypesOf(FetChSweep::class.java)
                    .map { Config(it.getConstructor().newInstance()) }
                    .sortedBy { it.name }

    class Config<T>(private val example: FetChSweep<T>) {

        val name   = example.name
        val image  = example.image
        val mClass = example::class

        fun create(): FetChSweep<T> {
            return example::class.primaryConstructor!!.call()
        }

    }

}