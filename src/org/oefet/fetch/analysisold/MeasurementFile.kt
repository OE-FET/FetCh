package org.oefet.fetch.analysisold

import jisa.experiment.ResultList

class MeasurementFile(path: String) {

    val data = ResultList.loadFile(path)

    init {

        if (data.getAttribute("type") == null) {
            throw Exception("That is not a measurement file.")
        }

    }

    fun getType(): CurveType {

        return when (data.getAttribute("type")) {
            "output"   -> CurveType.OUTPUT
            "transfer" -> CurveType.TRANSFER
            else       -> CurveType.UNKNOWN
        }

    }

    fun getTCurve() : TCurve {

        return TCurve(data)

    }

    fun getOCurve() : OCurve {

        return OCurve(data)

    }

    fun getCurve(): Curve {

        return when (getType()) {

            CurveType.OUTPUT   -> getOCurve()
            CurveType.TRANSFER -> getTCurve()
            else               -> throw Exception("Cannot determine curve type!")

        }

    }

    enum class CurveType {
        OUTPUT,
        TRANSFER,
        UNKNOWN
    }

}