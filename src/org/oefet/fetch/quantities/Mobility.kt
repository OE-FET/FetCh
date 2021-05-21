package org.oefet.fetch.quantities

abstract class Mobility : Quantity {

    override val symbol = "μ"
    override val unit   = "cm^2/Vs"
    override val important  = false

}