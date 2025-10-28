package org.oefet.fetch.gui.elements

import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import jisa.Util
import jisa.gui.Colour
import jisa.gui.DataDisplay
import jisa.gui.Grid
import jisa.gui.JFXElement
import jisa.gui.Table
import org.oefet.fetch.data.FetChData
import org.oefet.fetch.gui.tabs.Results
import org.oefet.fetch.quant.DoubleQuantity
import org.oefet.fetch.quant.StringQuantity
import kotlin.math.absoluteValue

class ResultDisplay(title: String, val data: FetChData) : JFXElement(title) {

    private val mainVBox      = VBox()
    private val titleHBox     = HBox()
    private val icon          = ImageView()
    private val titleVBox     = VBox()
    private val titleLabel    = Label()
    private val subTitleLabel = Label()
    private val viewButton    = Button("Open")
    private val remButton     = Button("Remove")
    private val results       = GridPane()
    private val tags          = GridPane()

    init {

        setCentreNode(mainVBox)

        tags.vgap = 5.0
        tags.hgap = 5.0

        results.vgap = 5.0
        results.hgap = 5.0

        results.columnConstraints.addAll(
            ColumnConstraints().apply { percentWidth = 50.0 },
            ColumnConstraints().apply { percentWidth = 50.0 }
        )

        tags.columnConstraints.addAll(
            ColumnConstraints().apply { percentWidth = 50.0 },
            ColumnConstraints().apply { percentWidth = 50.0 }
        )

        mainVBox.children.addAll(titleHBox, Separator(), Label("Results"), results, Separator(), Label("Parameters"), tags)
        titleHBox.children.addAll(icon, titleVBox, VBox(viewButton, remButton))
        titleVBox.children.addAll(titleLabel, subTitleLabel)

        HBox.setHgrow(titleVBox, Priority.ALWAYS)
        viewButton.maxWidth = Double.MAX_VALUE
        remButton.maxWidth = Double.MAX_VALUE

        titleHBox.spacing = 10.0
        mainVBox.spacing  = 10.0

        titleLabel.maxWidth    = Double.MAX_VALUE
        subTitleLabel.maxWidth = Double.MAX_VALUE

        icon.image = data.icon
        titleLabel.font = Font.font(titleLabel.font.name, FontWeight.BOLD, 24.0)

        titleLabel.text = data.name
        subTitleLabel.text = data.findParameter("Name", StringQuantity::class)?.value ?: "Unknown Device"

        var i = 0
        for ((name, list) in data.results.groupBy { it.name }) {

            val text = if (list.size == 1) {

                val result = list.first()

                when (result.value) {

                    is Number -> "%s = %.02e %s".format(result.symbol, result.value, result.type.units)
                    is String -> "%s = %s".format(result.symbol, result.value)
                    else      -> "%s = %s %s".format(result.symbol, result.value, result.type.units)

                }
            } else {

                val min = list.minOf { it.value }
                val max = list.maxOf { it.value }

                "%s = %.02e to %.02e %s".format(list.first().symbol, min, max, list.first().type.units)

            }

            val tag = Label(text);
            tag.background = Background(BackgroundFill(Colour.LIGHTGREEN, CornerRadii(3.0), Insets.EMPTY))
            tag.maxWidth   = Double.MAX_VALUE
            tag.padding    = Insets(5.0)
            results.add(tag, i % 2, i / 2)

            GridPane.setHgrow(tag, Priority.ALWAYS)

            i++

        }

        for ((i, parameter) in data.parameters.withIndex()) {

            val text = when (parameter.value) {

                is Number -> "%s = %.02e %s".format(parameter.symbol, parameter.value, parameter.type.units)
                is String -> "%s = %s".format(parameter.symbol, parameter.value)
                else      -> "%s = %s %s".format(parameter.symbol, parameter.value, parameter.type.units)

            }

            val tag = Label(text);

            tag.background = Background(BackgroundFill(Colour.LIGHTBLUE, CornerRadii(3.0), Insets.EMPTY))
            tag.maxWidth   = Double.MAX_VALUE
            tag.padding    = Insets(5.0)
            tags.add(tag, i % 2, i / 2)

            GridPane.setHgrow(tag, Priority.ALWAYS)

            viewButton.onAction = EventHandler { open() }

        }

        val tooltip = Tooltip()
        tooltip.contentDisplay = ContentDisplay.GRAPHIC_ONLY

        val display = data.getDisplay()

        if (display is JFXElement) {
            display.clearToolbar()
        }

        tooltip.graphic = display.node

        Tooltip.install(icon, tooltip)

    }

    fun open() {

        val grid    = Grid(data.name, 1)
        val topRow  = Grid(2)
        val topLeft = Grid(1)

        val display = data.getDisplay()
        val results = DataDisplay("Results")
        val params  = DataDisplay("Parameters")

        data.results.groupBy { it.name }.forEach { (name, list) -> when {

            list.size > 1                        -> { results.addParameter(name, "%.02e to %.02e %s".format(list.minOf { it.value }, list.maxOf { it.value }, list.first().type.units)) }
            list.first().error.absoluteValue > 0 -> { results.addParameter(name, "%.02e ± %.02e %s".format(list.first().value, list.first().error, list.first().type.units)) }
            else                                 -> { results.addParameter(name, "%.02e %s".format(list.first().value, list.first().type.units)) }

        } }

        data.parameters.forEach { when {

            it is DoubleQuantity && it.error.absoluteValue > 0 -> { params.addParameter(it.name, "%.02e ± %.02e %s".format(it.value, it.error, it.type.units)) }
            it is DoubleQuantity && it.error == 0.0            -> { params.addParameter(it.name, "%.02e %s".format(it.value, it.type.units)) }
            else                                               -> { params.addParameter(it.name, "%s %s".format(it.value, it.type.units)) }

        } }

        topLeft.addAll(results, params)
        topRow.addAll(topLeft, display)
        grid.addAll(topRow, Table("Data", data.data))

        Results.addCloseable(grid)
        Results.select(grid)

    }

}