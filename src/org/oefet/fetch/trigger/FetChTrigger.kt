package org.oefet.fetch.trigger

import org.oefet.fetch.FetChEntity

abstract class FetChTrigger : FetChEntity() {

    abstract fun isTriggered(event: Event): Boolean

    enum class Event {
        QUEUE_START,
        QUEUE_STOP,
        QUEUE_ADVANCE,
        QUANTITY_UPDATE
    }

}