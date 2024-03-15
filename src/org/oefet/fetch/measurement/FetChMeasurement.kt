package org.oefet.fetch.measurement

import javafx.scene.image.Image
import jisa.enums.Icon
import jisa.results.ResultTable
import org.oefet.fetch.FetChEntity
import org.oefet.fetch.results.FetChResult
import org.oefet.fetch.results.SimpleResult
import kotlin.reflect.KClass

abstract class FetChMeasurement(private val name: String, fileLabel: String, val tag: String, override val image: Image) : FetChEntity() {

    constructor(name: String, tag: String)               : this(name, tag, tag, Icon.FLASK.blackImage)
    constructor(name: String)                            : this(name, name.replace(" ", ""))
    constructor(name: String, tag: String, image: Image) : this(name, tag, tag, image)

    private val labelProperty = StringParameter("Basic", "Name", null, fileLabel)

    infix fun <T: Any> Any?.takeIf(type: KClass<T>): T? {
        return this.takeIf { type.isInstance(this) } as T?
    }

    open fun processResults(data: ResultTable): FetChResult {
        return SimpleResult(name, tag, data)
    }

    override fun start() {
        results.setAttribute("Type", tag)
        super.start()
    }

    override fun getName(): String {
        return this.name
    }

    override fun getLabel(): String {
        return labelProperty.value
    }

    override fun setLabel(value: String) {
        labelProperty.value = value
    }

    abstract override fun run(results: ResultTable)

    abstract override fun onFinish()

    override fun onError() {

    }

    override fun onInterrupt() {

    }

}

