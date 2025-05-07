package org.oefet.fetch.gui.elements

import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import jisa.enums.Icon
import jisa.gui.GUI
import jisa.gui.Grid
import jisa.gui.JFXElement
import jisa.gui.ListDisplay
import org.oefet.fetch.logging.DashValue
import org.oefet.fetch.logging.Log
import org.oefet.fetch.logging.Source

class LogActionCreator(title: String) : JFXElement(title) {

    private val hbox       = HBox()
    private val vboxLeft   = VBox()
    private val vboxRight  = VBox()
    private val label      = Label("Enter expression...")
    private val available  = ListDisplay<Source>("Available Sources")
    private val list       = ListDisplay<Source>("Selected Sources")
    private val titleLabel = Label("Name:")
    private val unitsLabel = Label("Units:")
    private val title      = TextField()
    private val units      = TextField()
    private val text       = TextArea()
    private val addButton  = Button("Add")
    private val remButton  = Button("Remove")

    init {

        vboxLeft.spacing  = GUI.SPACING
        vboxRight.spacing = GUI.SPACING
        hbox.spacing      = GUI.SPACING

        vboxLeft.children.addAll(HBox(titleLabel, this.title).apply {spacing = GUI.SPACING }, HBox(unitsLabel, this.units).apply {spacing = GUI.SPACING }, label, text)
        vboxRight.children.addAll(list.borderedNode)
        hbox.children.addAll(JFXElement("Expression", vboxLeft).borderedNode, vboxRight)

        VBox.setVgrow(text, Priority.ALWAYS)
        VBox.setVgrow(label, Priority.NEVER)
        VBox.setVgrow(vboxRight.children[0], Priority.ALWAYS)
        HBox.setHgrow(vboxRight, Priority.ALWAYS)
        HBox.setHgrow(vboxLeft, Priority.NEVER)
        HBox.setHgrow(this.title, Priority.ALWAYS)
        HBox.setHgrow(titleLabel, Priority.NEVER)
        HBox.setHgrow(this.units, Priority.ALWAYS)
        HBox.setHgrow(unitsLabel, Priority.NEVER)

        Log.getSources().forEach { available.add(it, it.name, "Data Source", Icon.DASHBOARD.blackImage) }

        unitsLabel.minWidthProperty().bind(titleLabel.widthProperty())

        val letters = arrayOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z")

        val window = Grid("Add Source", available)

        list.addToolbarButton("Add") {

            if (window.showAsConfirmation()) {

                val selected = available.selected.getObject()

                if (selected !in list.items.map { it.getObject() }) {
                    list.add(
                        selected,
                        "${letters[list.size()]} = ${selected.name}",
                        "Data Source",
                        Icon.CONNECTION.blackImage
                    )
                }

            }

        }

        list.addToolbarButton("Remove") {
            list.remove(list.selected)
        }

        hbox.setPrefSize(1024.0, 600.0)

        setCentreNode(hbox)

    }

    fun create(): DashValue = DashValue(title.text, units.text, list.items.map{ it.getObject() }, text.text)

}