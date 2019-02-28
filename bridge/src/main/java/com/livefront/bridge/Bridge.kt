package com.livefront.bridge

import android.content.Context
import android.os.Build.VERSION_CODES
import android.os.Bundle

object Bridge {

    private var sDelegate: BridgeDelegate? = null

    private fun checkInitialization() {
        if (sDelegate == null) {
            throw IllegalStateException(
                    "You must first call initialize before calling any other methods")
        }
    }

    /**
     * Clears any data associated with the given target object that may be stored to disk. This
     * will not affect data stored for restoration after configuration changes. Due to how these
     * changes are monitored, this method will have no affect prior to
     * [VERSION_CODES.ICE_CREAM_SANDWICH].
     *
     *
     * It is required to call [.initialize] before calling this
     * method.
     */
    fun clear(target: Any) {
        checkInitialization()
        sDelegate!!.clear(target)
    }

    /**
     * Clears all data from disk and memory. Does not require a call to [.initialize].
     */
    fun clearAll(context: Context) {
        if (sDelegate != null) {
            sDelegate!!.clearAll()
        }
    }

    /**
     * Initializes the framework used to save and restore data and route it to a location free from
     * [android.os.TransactionTooLargeException].
     *
     * @param context           an application [Context] necessary for saving state to disk
     */
    fun initialize(context: Context) {
        sDelegate = BridgeDelegate(context)
    }

    /**
     * Restores the state of the given target object based on tracking information stored in the
     * given [Bundle]. The actual saved data will be retrieved from a location in memory or
     * stored on disk.
     *
     *
     * It is required to call [.initialize] before calling this
     * method.
     */
    fun restoreInstanceState(target: Any, state: Bundle?, stateHandler: (Bundle) -> Unit) {
        checkInitialization()
        sDelegate!!.restoreInstanceState(target, state, stateHandler)
    }

    /**
     * Saves the state of the given target object to a location in memory and disk and stores
     * tracking information in given [Bundle].
     *
     *
     * It is required to call [.initialize] before calling this
     * method.
     */
    fun saveInstanceState(target: Any, state: Bundle, stateHandler: (Bundle) -> Unit) {
        checkInitialization()
        sDelegate!!.saveInstanceState(target, state, stateHandler)
    }

}
