package org.oefet.fetch.gui.images

import javafx.scene.image.Image
import java.net.URL

object Images {

    fun getURL(path: String) : URL = javaClass.getResource(path)

    fun getImage(path: String) : Image = Image(getURL(path).toExternalForm())

}