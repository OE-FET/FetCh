package org.oefet.fetch.quantities

abstract class Mobility : DoubleQuantity {

    override val symbol = "μ"
    override val unit   = "cm^2/Vs"
    override val important  = false

}