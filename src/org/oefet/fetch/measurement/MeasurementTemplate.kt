package org.oefet.fetch.measurement

/*
class MeasurementTemplate : FetChMeasurement("Measurement Name", "FileName") {

    /*  Measurement parameters go here, for instance:
     *
     *  Simple quantity (automatically determines type of input to use based on type of defaultValue):
     *      val param by userInput("Section Name", "Param Name [units]", defaultValue)
     *
     * Time input:
     *      val param by userTimeInput("Section Name", "Param Name", defaultMilliseconds)
     *
     * Dropdown/select/choice box:
     *      val param by userChoice("Section Name", "Choice Name", "Option 1", "Option 2", "Option 3", ...)
     */

    /*  Instruments go here, for instance:
     *
     *  Required instrument (i.e cannot be left empty):
     *      val instrument by requiredInstrument("Name", Type::class)
     *
     *  Optional instrument (i.e. can be left unselected by user):
     *      val instrument by optionalInstrument("Name", Type::class)
     */

    companion object : Columns() {

        /*  Columns for results table go here. For instance:
         *
         *  For numerical (decimal) columns:
         *      val COLUMN_NAME = decimalColumn("Name", "Units")
         *
         *  For integer columns:
         *      val COLUMN_NAME = integerColumn("Name", "Units")
         *
         *  For text columns:
         *      val COLUMN_NAME = textColumn("Name")
         *
         *  For true/false (boolean) columns:
         *      val COLUMN_NAME = booleanColumn("Name")
         */

    }

    /** Leave this here, but otherwise you can ignore it */
    override fun getColumns(): Array<Column<*>> = Companion.getColumns()

    /**
     * This is the function that is called when the measurement is run,
     * this is where you put the main logic of your measurement routine.
     */
    override fun run(results: ResultTable) {
        TODO("Write your measurement code here")
    }

    /**
     * This is the function that is called after the measurement has finished,
     * regardless of whether that was because it completed successfully,
     * was interrupted or encountered an error. This is where you should put
     * any shutdown code you need (i.e. turning things off etc).
     */
    override fun onFinish() {
        TODO("Write your shutdown code here")
    }

}
*/