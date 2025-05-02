package org.oefet.fetch.action


import javafx.scene.image.Image
import org.oefet.fetch.FetChEntity

abstract class FetChAction(private val name: String, override val image: Image) : FetChEntity() {


    override fun getName(): String {
        return this.name
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