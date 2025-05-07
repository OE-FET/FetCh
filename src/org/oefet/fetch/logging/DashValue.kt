package org.oefet.fetch.logging

import com.expression.parser.Parser
import jisa.control.ConfigBlock
import jisa.results.Column

class DashValue(
    val name: String,
    val units: String,
    sources: List<Source>,
    val expression: String
) {

    val sources     = sources.toMutableList()
    val letters     = arrayOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "w", "x", "y", "z").slice(0 until sources.size).toTypedArray()
    val column      = Column.ofDoubles(name, units)
    val value get() = Parser.eval(expression, letters, sources.map { it.cached }.toTypedArray())
    var isEnabled   = true

    fun writeToConfig(block: ConfigBlock) {

        block.stringValue("name").set(name)
        block.stringValue("units").set(units)
        block.stringValue("sources").set(sources.joinToString("::") { it.name })
        block.stringValue("expression").set(expression)

    }

    fun refreshSources() {
        val allSources = Log.getSources()
        sources.replaceAll { s -> allSources.find { it.name == s.name } ?: s }
    }

    fun usesSource(source: Source): Boolean {
        return source in sources
    }

    companion object {

        fun loadFromConfig(block: ConfigBlock): DashValue {

            val allSources = Log.getSources()

            val name       = block.stringValue("name").getOrDefault("Unknown")
            val units      = block.stringValue("units").getOrDefault("Unknown")
            val sources    = block.stringValue("sources").getOrDefault("").split("::").mapNotNull { n -> allSources.find { it.name == n } }
            val expression = block.stringValue("expression").getOrDefault("Unknown")

            return DashValue(name, units, sources, expression)

        }

    }

}