package com.livefront.bridge

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcel
import android.util.Base64
import java.util.*

internal class BridgeDelegate(context: Context) {

    private var mIsClearAllowed = false
    private var mIsFirstCreateCall = true
    private val mUuidBundleMap = HashMap<String, Bundle>()
    private val mObjectUuidMap = WeakHashMap<Any, String>()
    private val mSharedPreferences: SharedPreferences

    init {
        mSharedPreferences = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        registerForLifecycleEvents(context)
    }

    fun clear(target: Any) {
        if (!mIsClearAllowed) {
            return
        }
        val uuid = mObjectUuidMap.remove(target) ?: return
        clearDataForUuid(uuid)
    }

    fun clearAll() {
        mUuidBundleMap.clear()
        mObjectUuidMap.clear()
        mSharedPreferences.edit()
                .clear()
                .apply()
    }

    private fun clearDataForUuid(uuid: String) {
        mUuidBundleMap.remove(uuid)
        clearDataFromDisk(uuid)
    }

    private fun clearDataFromDisk(uuid: String) {
        mSharedPreferences.edit()
                .remove(getKeyForEncodedBundle(uuid))
                .apply()
    }

    private fun getKeyForEncodedBundle(uuid: String): String {
        return String.format(KEY_BUNDLE, uuid)
    }

    private fun getKeyForUuid(target: Any): String {
        return String.format(KEY_UUID, target.javaClass.name)
    }

    private fun readFromDisk(uuid: String): Bundle? {
        val encodedString = mSharedPreferences.getString(getKeyForEncodedBundle(uuid), null)
                ?: return null
        val parcelBytes = Base64.decode(encodedString, 0)
        val parcel = Parcel.obtain()
        parcel.unmarshall(parcelBytes, 0, parcelBytes.size)
        parcel.setDataPosition(0)
        val bundle = parcel.readBundle(BridgeDelegate::class.java.classLoader)
        parcel.recycle()
        return bundle
    }

    @SuppressLint("NewApi")
    private fun registerForLifecycleEvents(context: Context) {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(
                object : ActivityLifecycleCallbacksAdapter() {
                    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                        mIsClearAllowed = true

                        // Make sure we clear all data after creating the first Activity if it does
                        // does not have a saved stated Bundle. (During state restoration, the
                        // first Activity will always have a non-null saved state Bundle.)
                        if (!mIsFirstCreateCall) {
                            return
                        }
                        mIsFirstCreateCall = false
                        if (savedInstanceState == null) {
                            mSharedPreferences.edit()
                                    .clear()
                                    .apply()
                        }
                    }

                    override fun onActivityDestroyed(activity: Activity) {
                        // Don't allow clearing during known configuration changes (and other
                        // events unrelated to calling "finish()".)
                        mIsClearAllowed = activity.isFinishing
                    }
                }
        )
    }

    fun restoreInstanceState(target: Any, state: Bundle?, stateHandler: (Bundle) -> Unit) {
        if (state == null) {
            return
        }
        val uuid = (if (mObjectUuidMap.containsKey(target))
            mObjectUuidMap[target]
        else
            state.getString(getKeyForUuid(target), null)) ?: return
        mObjectUuidMap[target] = uuid
        val bundle = (if (mUuidBundleMap.containsKey(uuid))
            mUuidBundleMap[uuid]
        else
            readFromDisk(uuid)) ?: return
        stateHandler.invoke(bundle)
        clearDataForUuid(uuid)
    }

    fun saveInstanceState(target: Any, state: Bundle, stateHandler: (Bundle) -> Unit) {
        var uuid = mObjectUuidMap[target]
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            mObjectUuidMap.put(target, uuid)
        }
        state.putString(getKeyForUuid(target), uuid)
        val bundle = Bundle()
        stateHandler.invoke(bundle)
        if (bundle.isEmpty) {
            // Don't bother saving empty bundles
            return
        }
        mUuidBundleMap[uuid] = bundle
        writeToDisk(uuid, bundle)
    }

    private fun writeToDisk(uuid: String,
                            bundle: Bundle) {
        val parcel = Parcel.obtain()
        parcel.writeBundle(bundle)
        val encodedString = Base64.encodeToString(parcel.marshall(), 0)
        mSharedPreferences.edit()
                .putString(getKeyForEncodedBundle(uuid), encodedString)
                .apply()
        parcel.recycle()
    }

    companion object {

        private val TAG = BridgeDelegate::class.java.name

        private val KEY_BUNDLE = "bundle_%s"
        private val KEY_UUID = "uuid_%s"
    }

}