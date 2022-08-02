package org.oefet.fetch

import org.oefet.fetch.action.FetChAction
import org.reflections.Reflections
import kotlin.reflect.full.primaryConstructor

object Actions {

    val types = Reflections("org.oefet.fetch.action")
                    .getSubTypesOf(FetChAction::class.java)
                    .map { Config(it.getConstructor().newInstance()) }
                    .sortedBy { it.name }

    class Config(private val example: FetChAction) {

        val name   = example.name
        val image  = example.image
        val mClass = example::class

        fun create(): FetChAction {
            return example::class.primaryConstructor!!.call()
        }

    }

}