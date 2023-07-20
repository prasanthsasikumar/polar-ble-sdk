package com.polar.androidblesdk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.util.Pair
import com.google.android.material.snackbar.Snackbar
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.PolarH10OfflineExerciseApi
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.util.*
import edu.ucsd.sccn.LSL.StreamInfo
import edu.ucsd.sccn.LSL.StreamOutlet
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat

class AHLActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "AHLActivity"
        private const val API_LOGGER_TAG = "API LOGGER"
        private const val PERMISSION_REQUEST_CODE = 1
    }

    // ATTENTION! Replace with the device ID from your device.
    //Device 2 - C3572720 | Device 1 - B21EE82C
    private var deviceId = "C3572720"

    private val api: PolarBleApi by lazy {
        // Notice all features are enabled
        PolarBleApiDefaultImpl.defaultImplementation(
            applicationContext,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
            )
        )
    }
    private lateinit var broadcastDisposable: Disposable
    private var scanDisposable: Disposable? = null
    private var autoConnectDisposable: Disposable? = null
    private var hrDisposable: Disposable? = null
    private var ecgDisposable: Disposable? = null
    private var accDisposable: Disposable? = null
    private var gyrDisposable: Disposable? = null
    private var magDisposable: Disposable? = null
    private var ppgDisposable: Disposable? = null
    private var ppiDisposable: Disposable? = null
    private var sdkModeEnableDisposable: Disposable? = null
    private var recordingStartStopDisposable: Disposable? = null
    private var recordingStatusReadDisposable: Disposable? = null
    private var listExercisesDisposable: Disposable? = null
    private var fetchExerciseDisposable: Disposable? = null
    private var removeExerciseDisposable: Disposable? = null
    private var isWriting = false

    private var sdkModeEnabledStatus = false
    private var deviceConnected = false
    private var bluetoothEnabled = false
    private var exerciseEntries: MutableList<PolarExerciseEntry> = mutableListOf()

    private lateinit var broadcastButton: Button
    private lateinit var connectButton: Button
    private lateinit var autoConnectButton: Button
    private lateinit var scanButton: Button
    private lateinit var hrButton: Button
    private lateinit var ecgButton: Button
    private lateinit var accButton: Button
    private lateinit var gyrButton: Button
    private lateinit var magButton: Button
    private lateinit var ppgButton: Button
    private lateinit var ppiButton: Button
    private lateinit var listExercisesButton: Button
    private lateinit var fetchExerciseButton: Button
    private lateinit var removeExerciseButton: Button
    private lateinit var startH10RecordingButton: Button
    private lateinit var stopH10RecordingButton: Button
    private lateinit var readH10RecordingStatusButton: Button
    private lateinit var setTimeButton: Button
    private lateinit var getTimeButton: Button
    private lateinit var toggleSdkModeButton: Button
    private lateinit var getDiskSpaceButton: Button

    //Verity Sense offline recording use
    private lateinit var listRecordingsButton: Button
    private lateinit var startRecordingButton: Button
    private lateinit var stopRecordingButton: Button
    private lateinit var downloadRecordingButton: Button
    private lateinit var deleteRecordingButton: Button
    private val entryCache: MutableMap<String, MutableList<PolarOfflineRecordingEntry>> = mutableMapOf()

    private val streamName = "MyStream"
    private val streamType = "EEG"
    private val channelCount = 8
    private val samplingRate = 55

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ahl)
        Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo())
        connectButton = findViewById(R.id.connect_button)
        ppgButton = findViewById(R.id.ohr_ppg_button)

        api.setPolarFilter(false)

        // If there is need to log what is happening inside the SDK, it can be enabled like this:
        val enableSdkLogs = false
        if(enableSdkLogs) {
            api.setApiLogger { s: String -> Log.d(API_LOGGER_TAG, s) }
        }

        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BLE power: $powered")
                bluetoothEnabled = powered
                if (powered) {
                    enableAllButtons()
                    showToast("Phone Bluetooth on")
                } else {
                    disableAllButtons()
                    showToast("Phone Bluetooth off")
                }
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: ${polarDeviceInfo.deviceId}")
                deviceId = polarDeviceInfo.deviceId
                deviceConnected = true
                val buttonText = getString(R.string.disconnect_from_device, deviceId)
                toggleButtonDown(connectButton, buttonText)
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                deviceConnected = false
                val buttonText = getString(R.string.connect_to_device, deviceId)
                toggleButtonUp(connectButton, buttonText)
                toggleButtonUp(toggleSdkModeButton, R.string.enable_sdk_mode)
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "DIS INFO uuid: $uuid value: $value")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "BATTERY LEVEL: $level")
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData.PolarHrSample) {
                // deprecated
            }
        })

        connectButton.text = getString(R.string.connect_to_device, deviceId)
        connectButton.setOnClickListener {
            try {
                if (deviceConnected) {
                    api.disconnectFromDevice(deviceId)
                } else {
                    api.connectToDevice(deviceId)
                }
            } catch (polarInvalidArgument: PolarInvalidArgument) {
                val attempt = if (deviceConnected) {
                    "disconnect"
                } else {
                    "connect"
                }
                Log.e(TAG, "Failed to $attempt. Reason $polarInvalidArgument ")
            }
        }

        var outputStreamWriter: OutputStreamWriter? = null
        var fileOutputStream: FileOutputStream? = null
        ppgButton.setOnClickListener {
            val isDisposed = ppgDisposable?.isDisposed ?: true
            Log.d(TAG, "isWriting????: $isWriting")
            if(isWriting){
                isWriting = false
                outputStreamWriter?.close()
                fileOutputStream?.close()
                Log.d(TAG, "PPG file closed")
            }

            if(!isWriting){
               var sdf = SimpleDateFormat("ddMMyyyyHHmmss", Locale.getDefault())
               var currentDateandTime = sdf.format(Date())
               var fileName = "PolarData$currentDateandTime.csv"
               var file = File(this.getExternalFilesDir(null), fileName)
               fileOutputStream = FileOutputStream(file, true)
               outputStreamWriter = OutputStreamWriter(fileOutputStream)
               Log.d(TAG, "PPG file created")
               outputStreamWriter?.append("ppg0, ppg1, ppg2, ambient, Timestamp")
               outputStreamWriter?.append("\n")
               isWriting = true
           }
            if (isDisposed) {
                toggleButtonDown(ppgButton, R.string.stop_ppg_stream)
                ppgDisposable =
                    requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.PPG)
                        .flatMap { settings: PolarSensorSetting ->
                            api.startPpgStreaming(deviceId, settings)
                        }
                        .subscribe(
                            { polarPpgData: PolarPpgData ->
                                if (polarPpgData.type == PolarPpgData.PpgDataType.PPG3_AMBIENT1) {
                                    for (data in polarPpgData.samples) {
                                        Log.d(TAG, "PPG    ppg0: ${data.channelSamples[0]} ppg1: ${data.channelSamples[1]} ppg2: ${data.channelSamples[2]} ambient: ${data.channelSamples[3]} timeStamp: ${data.timeStamp}")
                                        outputStreamWriter?.append("${data.channelSamples[0]}, ${data.channelSamples[1]}, ${data.channelSamples[2]}, ${data.channelSamples[3]}, ${data.timeStamp} \n")
                                    }
                                }
                            },
                            { error: Throwable ->
                                toggleButtonUp(ppgButton, R.string.start_ppg_stream)
                                Log.e(TAG, "PPG stream failed. Reason $error")
                                outputStreamWriter?.close()
                                fileOutputStream?.close()
                            },
                            { Log.d(TAG, "PPG stream complete")
                                outputStreamWriter?.close()
                                fileOutputStream?.close()
                                Log.d(TAG, "PPG file closed")
                                isWriting = false
                            }
                        )
            } else {
                outputStreamWriter?.close()
                fileOutputStream?.close()
                toggleButtonUp(ppgButton, R.string.start_ppg_stream)
                // NOTE dispose will stop streaming if it is "running"
                ppgDisposable?.dispose()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (index in 0..grantResults.lastIndex) {
                if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                    disableAllButtons()
                    Log.w(TAG, "No sufficient permissions")
                    showToast("No sufficient permissions")
                    return
                }
            }
            Log.d(TAG, "Needed permissions are granted")
            enableAllButtons()
        }
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        api.foregroundEntered()
    }

    public override fun onDestroy() {
        super.onDestroy()
        api.shutDown()
    }

    private fun toggleButtonDown(button: Button, text: String? = null) {
        toggleButton(button, true, text)
    }

    private fun toggleButtonDown(button: Button, @StringRes resourceId: Int) {
        toggleButton(button, true, getString(resourceId))
    }

    private fun toggleButtonUp(button: Button, text: String? = null) {
        toggleButton(button, false, text)
    }

    private fun toggleButtonUp(button: Button, @StringRes resourceId: Int) {
        toggleButton(button, false, getString(resourceId))
    }

    private fun toggleButton(button: Button, isDown: Boolean, text: String? = null) {
        if (text != null) button.text = text

        var buttonDrawable = button.background
        buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
        if (isDown) {
            DrawableCompat.setTint(buttonDrawable, resources.getColor(R.color.primaryDarkColor))
        } else {
            DrawableCompat.setTint(buttonDrawable, resources.getColor(R.color.primaryColor))
        }
        button.background = buttonDrawable
    }

    private fun requestStreamSettings(identifier: String, feature: PolarBleApi.PolarDeviceDataType): Flowable<PolarSensorSetting> {
        val availableSettings = api.requestStreamSettings(identifier, feature)
        val allSettings = api.requestFullStreamSettings(identifier, feature)
            .onErrorReturn { error: Throwable ->
                Log.w(TAG, "Full stream settings are not available for feature $feature. REASON: $error")
                PolarSensorSetting(emptyMap())
            }
        return Single.zip(availableSettings, allSettings) { available: PolarSensorSetting, all: PolarSensorSetting ->
            if (available.settings.isEmpty()) {
                throw Throwable("Settings are not available")
            } else {
                Log.d(TAG, "Feature " + feature + " available settings " + available.settings)
                Log.d(TAG, "Feature " + feature + " all settings " + all.settings)
                return@zip android.util.Pair(available, all)
            }
        }
            .observeOn(AndroidSchedulers.mainThread())
            .toFlowable()
            .flatMap { sensorSettings: android.util.Pair<PolarSensorSetting, PolarSensorSetting> ->
                DialogUtility.showAllSettingsDialog(
                    this@AHLActivity,
                    sensorSettings.first.settings,
                    sensorSettings.second.settings
                ).toFlowable()
            }
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        toast.show()
    }

    private fun showSnackbar(message: String) {
        val contextView = findViewById<View>(R.id.buttons_container)
        Snackbar.make(contextView, message, Snackbar.LENGTH_LONG)
            .show()
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                // Respond to positive button press
            }
            .show()
    }

    private fun disableAllButtons() {
        connectButton.isEnabled = false
        ppgButton.isEnabled = false
    }

    private fun enableAllButtons() {
        connectButton.isEnabled = true
        ppgButton.isEnabled = true
    }

    private fun disposeAllStreams() {
        ecgDisposable?.dispose()
        accDisposable?.dispose()
        gyrDisposable?.dispose()
        magDisposable?.dispose()
        ppgDisposable?.dispose()
        ppgDisposable?.dispose()
    }
}