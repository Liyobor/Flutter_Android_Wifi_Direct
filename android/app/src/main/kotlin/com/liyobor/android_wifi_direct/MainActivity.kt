package com.liyobor.android_wifi_direct


import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity: FlutterActivity() {

    object Constants {
        const val REQUEST_WIFI = 87
        const val PERMISSION_ID = 1010
    }

    companion object {
        const val UPLOAD_FILE_URL =
//            "https://prsrbrubsj.execute-api.ap-northeast-1.amazonaws.com/demo/upload"
            "https://9nxqbm8t84.execute-api.ap-northeast-1.amazonaws.com/demo/upload"
    }


    private val methodChannel = "com.liyobor.android_wifi_direct.method"
    private val eventChannel = "com.liyobor.android_wifi_direct.event"

    private val streamHandler = EventStreamHandler()
//
    private val intentFilter = IntentFilter().apply {
    addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
    addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
    addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
    addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private lateinit var wManager: WifiP2pManager


    private var wChannel: WifiP2pManager.Channel? = null
    private var wReceiver: BroadcastReceiver? = null
    private val peers = mutableListOf<WifiP2pDevice>()

    private val nServerPort = 8888


    private lateinit var webSocketServer: WebSocketServer
    private lateinit var webSocketClient: WebSocketClient

    private var outputStream: BufferedOutputStream? = null



    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)
            val deviceList = mutableListOf<String>()
            for (device in refreshedPeers){
//                Timber.i("device = ${device.deviceName}")
                deviceList.add(device.deviceName)
            }
            streamHandler.onWifiScanResult(deviceList)


            // If an AdapterView is backed by this data, notify it
            // of the change. For instance, if you have a ListView of
            // available peers, trigger an update.
//            (listAdapter as WiFiPeerListAdapter).notifyDataSetChanged()

            // Perform any other updates needed based on the new list of
            // peers connected to the Wi-Fi P2P network.
        }

        if (peers.isEmpty()) {
            Timber.i("No devices found")
            return@PeerListListener
        }


    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.uprootAll()
        Timber.plant(Timber.DebugTree())

        wManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        wChannel = wManager.initialize(this, mainLooper, null)



        wManager.removeGroup(wChannel,object:WifiP2pManager.ActionListener{
            override fun onSuccess() {

            }

            override fun onFailure(p0: Int) {

            }
        })


    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wReceiver)
    }

    override fun onResume() {
        super.onResume()
        wReceiver = WiFiBroadcastReceiver(wManager, wChannel, this)
        registerReceiver(wReceiver, intentFilter)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.PERMISSION_ID -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Timber.i("Debug:","You have the location Permission")
                }
                else{
                    Timber.i("denied location !!! $requestCode -> ${grantResults[0]}")
                }

            }
            Constants.REQUEST_WIFI ->{
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Timber.i("You have the WIFI Permission")
                }
                else{
                    Timber.i("WIFI denied !!! $requestCode -> ${grantResults[0]}")
                }
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }






    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, eventChannel).setStreamHandler(
            streamHandler
        )
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            methodChannel
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "discoverPeers" -> {
//                    streamHandler.onResult(listOf(12f,0.85f))


                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            ),
                            Constants.PERMISSION_ID
                        )

                    }else{
                        wManager.discoverPeers(
                            wChannel, object : WifiP2pManager.ActionListener {

                                override fun onSuccess() {
                                    // Code for when the discovery initiation is successful goes here.
                                    // No services have actually been discovered yet, so this method
                                    // can often be left blank. Code for peer discovery goes in the
                                    // onReceive method, detailed below.
                                    Timber.i("discoverPeers onSuccess")
                                }

                                override fun onFailure(reasonCode: Int) {
                                    // Code for when the discovery initiation fails goes here.
                                    // Alert the user that something went wrong.

                                    Timber.i("discoverPeers onFailure")
                                }
                            }
                        )
                    }


                }

                "connectToDevice"-> {
                    val index: Int = call.arguments()!!


                    val device = peers[index]
                    val config = WifiP2pConfig().apply {
                        deviceAddress = device.deviceAddress
                        wps.setup = WpsInfo.PBC
                    }


                    wManager.connect(wChannel,config,object :WifiP2pManager.ActionListener{
                        override fun onSuccess() {
                            Timber.i("connect success")


                        }

                        override fun onFailure(p0: Int) {
                            Timber.i("connect fail")
                        }
                    })
                }

                "sendMessage" -> {
                    wManager.requestGroupInfo(wChannel){
                            group ->
                        if(group.isGroupOwner){
                            webSocketServer.serverSend("hello client!")
                        }else{
                            if(!this::webSocketClient.isInitialized){
                                Timber.i("webSocketClient is not initialized")
                            }else{
                                webSocketClient.clientSend("hello server!")
                            }
                        }
                    }




                }

                "loadWav" -> {
                    loadWavFile(context)
                }
                else -> result.notImplemented()
            }
        }

    }

    private fun loadWavFile(context: Context){
        try {
            val inputStream: InputStream = context.resources.openRawResource(R.raw.pca)
//            val wavData = ByteArray(inputStream.available())
            val wavData = inputStream.readBytes()
            val pcmData = ByteArray(wavData.size-44)
            System.arraycopy(wavData,44,pcmData,0,pcmData.size)
            val adpcm = Adpcm()
            val encodedData = mutableListOf<Int>()
            var compressedNibble:Byte =0
            for((index,data) in pcmData.withIndex()){
                compressedNibble = (compressedNibble.toInt() shr 4).toByte()
                compressedNibble = (adpcm.adpcm3Encode(data) or compressedNibble.toInt()).toByte()
//                val encodeByte = adpcm.adpcm3Encode(data)
                encodedData.add(compressedNibble.toInt())
            }
            adpcm.encodeStateReset()
            Timber.i("---encode finish---")
            val decodedData = mutableListOf<Float>()

            for((index,data) in encodedData.withIndex()){
                val decodeByte = adpcm.adpcm3Decode(data)
                decodedData.add((decodeByte))
            }
            adpcm.decodeStateReset()

            val fileTemp = File.createTempFile("tmp_",".raw")
            if(!fileTemp.exists()){
                Timber.i("createTempFile failed!")
                return
            }
            val output = FileOutputStream(fileTemp)
            outputStream = BufferedOutputStream(output)

            val audioData = decodedData.toTypedArray().toFloatArray()
            val bytes = ByteArray(decodedData.size*4)
            ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asFloatBuffer().put(audioData)
            outputStream?.write(bytes)
            outputStream?.close()
            convert2Wav(fileTemp,decodedData.size*4)

            inputStream.close()

        }catch (e: Exception){
            e.printStackTrace()
        }
    }


    private fun uploadWav(wavCache:ByteArray?){
        val currentFormattedTime = getCurrentTime()
        val client = OkHttpClient()
        val uuidString = getUUID(context)
        try {
            val body = wavCache!!.toRequestBody("audio/wav".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(
                    "$UPLOAD_FILE_URL?filename=${
                        Build.MODEL + "_16BIT_WifiP2PTest/" + currentFormattedTime + "_" + uuidString.substring(
                            0,
                            8
                        )
                    }_adpcmMode3.wav"
                )
                .post(body).build()
            val response = client.newCall(request).execute()
            Timber.e("Response $response")
            response.close()
        } catch (e: Exception) {
            Timber.i("http time out $e")
        }
    }

    private fun convert2Wav(fileTmp:File,fileSize:Int) {
        Thread {
            try {
                val raw = fileTmp.readBytes().copyOfRange(0, fileSize)
                var wavCache:ByteArray? = WavFileBuilderKotlin()
                    .setAudioFormat(WavFileBuilderKotlin.WAVE_FORMAT_PCM)
                    .setSampleRate(16000)
                    .setBitsPerSample(WavFileBuilderKotlin.BITS_PER_SAMPLE_32)
                    .setNumChannels(WavFileBuilderKotlin.CHANNELS_MONO)
                    .setSubChunk1Size(WavFileBuilderKotlin.SUB_CHUNK_1_SIZE_PCM)
                    .build(raw)
                if (isNetworkAvailable(context)) {
                    uploadWav(wavCache)
                } else {
                    wavCache = null
                }
            } catch (e: FileNotFoundException) {
                Timber.i("file not found exception %s", e.localizedMessage)
            }
        }.start()
    }






    inner class WiFiBroadcastReceiver constructor(
        private val manager: WifiP2pManager?,
        private val channel: WifiP2pManager.Channel?,
        private val activity: FlutterActivity
    ) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {

                    Timber.i("WIFI_P2P_STATE_CHANGED_ACTION")

                    // Determine if Wifi P2P mode is enabled or not, alert
                    // the Activity.
                    when(intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)){
                        WifiP2pManager.WIFI_P2P_STATE_ENABLED ->{
                            // Wifi P2P is enabled
                            Timber.i("P2P is enabled")
                        }
                        else -> {
                            // Wi-Fi P2P is not enabled
                            Timber.i("P2P is not enabled")
                        }
                    }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {


                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            ),
                            Constants.PERMISSION_ID
                        )
                    }else{
                        wManager.requestPeers(wChannel, peerListListener)
                        Timber.i("P2P peers changed")
                    }

                    // The peer list has changed! We should probably do something about
                    // that.

                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {

                    Timber.i("WIFI_P2P_CONNECTION_CHANGED_ACTION")

                    wManager.requestConnectionInfo(wChannel
                    ) { p0 ->
                        if (p0 != null) {
                            wManager.requestGroupInfo(wChannel
                            ) { wifiP2pGroup ->
                                if (wifiP2pGroup != null) {
//                                    Timber.i("wifiP2pGroup = ${wifiP2pGroup.clientList}")
                                    Timber.i("isGroupOwner = ${p0.isGroupOwner}")

                                    if(!p0.isGroupOwner){
                                        if(!this@MainActivity::webSocketClient.isInitialized){
                                            webSocketClient = WebSocketClient(this@MainActivity,p0.groupOwnerAddress.hostAddress,nServerPort)
                                        }
                                    }else{
                                        if(!this@MainActivity::webSocketServer.isInitialized){
                                            webSocketServer = WebSocketServer(this@MainActivity)
                                        }
                                    }
                                }
                            }
                        }
                    }


                    // Connection state changed! We should probably do something about
                    // that.

                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {

                    Timber.i("WIFI_P2P_THIS_DEVICE_CHANGED_ACTION")

                }
            }
        }


    }


    inner class EventStreamHandler : EventChannel.StreamHandler{
        private var eventSink: EventChannel.EventSink? = null
        override fun onListen(p0: Any?, p1: EventChannel.EventSink?) {
            eventSink = p1
        }

        override fun onCancel(p0: Any?) {
            eventSink = null
        }

        fun onWifiScanResult(list: List<String>){
            Handler(Looper.getMainLooper() ?: return).post {
                eventSink?.success(mapOf("wifiList" to list))
            }
        }


    }
}
