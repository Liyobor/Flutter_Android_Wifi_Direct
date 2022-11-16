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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import timber.log.Timber
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class MainActivity: FlutterActivity() {

    object Constants {
        const val REQUEST_WIFI = 87
        const val PERMISSION_ID = 1010
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

//    private lateinit var wManager: WifiP2pManager
//    private val wManager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
//        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
//    }

    private lateinit var wManager: WifiP2pManager



    private var wChannel: WifiP2pManager.Channel? = null
    private var wReceiver: BroadcastReceiver? = null
//    private lateinit var wChannel: WifiP2pManager.Channel
//    private lateinit var wReceiver: WiFiBroadcastReceiver
    private val peers = mutableListOf<WifiP2pDevice>()

    private var groupOwnerAddress: String? = null
    private val nServerPort = 8888
    private var serverSocket: ServerSocket? = null

    private lateinit var clientInputStream:InputStream
    private lateinit var clientOutputStream:OutputStream
    private lateinit var serverInputStream:InputStream
    private lateinit var serverOutputStream:OutputStream

    private val clientSocket = Socket()


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


    private fun createClientThread(host:String?,port:Int):Thread?{
        if (host==null){
            Timber.i("host can not be null!")
            return null
        }

        val clientThread = Thread {
            if(!clientSocket.isBound){
                clientSocket.bind(null)
            }
            clientSocket.connect(InetSocketAddress(host,port),500)
            try {

                clientOutputStream = clientSocket.getOutputStream()
                clientOutputStream.write("message from client".toByteArray())
                while (clientSocket.isConnected){
                    clientInputStream = clientSocket.getInputStream()
                    val bufferedReader = BufferedReader(InputStreamReader(clientInputStream))
                    var stringLine: String?
                    while(!clientSocket.isClosed ){
                        stringLine = bufferedReader.readLine()
                        if(stringLine==null){
                            break
                        }
                        Timber.i("Read from server : $stringLine")
                    }
                }

                Timber.i("client socket disconnect")
//                outputStream.close()

            } catch (e: IOException) {
                e.printStackTrace()
                return@Thread
            }
//            finally {
//                socket.takeIf { it.isConnected }?.apply {
//                    close()
//                }
//            }
        }
        return clientThread
    }

    private fun clientSend(message:String){
        if(this::clientOutputStream.isInitialized){
            Timber.i("clientSend")
            Thread {
                clientOutputStream.write(message.toByteArray())
                clientOutputStream.close()
            }.start()

        }else{
            Timber.i("clientOutputStream is not isInitialized")
        }
    }

    private fun serverSend(message:String){

        if(this::serverOutputStream.isInitialized){
            Timber.i("serverSend")
            Thread {
                serverOutputStream.write(message.toByteArray())
                serverOutputStream.close()
            }.start()

        }else{
            Timber.i("serverOutputStream is not isInitialized")
        }
    }


    private fun createServerSocketThread():Thread?{
        try {
            // Open a server socket

            serverSocket = ServerSocket(nServerPort)

            // Start a server thread to do socket-accept tasks
            val serverThread = Thread {
                while (!serverSocket!!.isClosed){
                    try {
                        Timber.i("Waiting for client connection…")
                        val socketClient = serverSocket!!.accept()
                        Timber.i("Accepted connection from ${socketClient.inetAddress.hostAddress}")
                        serverInputStream = socketClient.getInputStream()
                        serverOutputStream = socketClient.getOutputStream()
                        val bufferedReader = BufferedReader(InputStreamReader(serverInputStream))
                        var stringLine:String? = null
                        while(!socketClient.isClosed ){
                            stringLine = bufferedReader.readLine()
                            if(stringLine==null){
                                break
                            }
                            Timber.i("Read from client = $stringLine")
                        }

//                        inputStream.close()
//                        outputStream.close()
                        bufferedReader.close()

                        Looper.prepare()
                        Toast.makeText(this,"receive message :$stringLine",Toast.LENGTH_LONG).show()
//                        Timber.i("Read data from client ok. Close connection from " + socketClient.inetAddress.hostAddress)
                        socketClient.close()
                    }catch (e:IOException){
                        e.printStackTrace()
                    }
                }
                Timber.i("server socket close")
            }
            return serverThread
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.uprootAll()
        Timber.plant(Timber.DebugTree())





//

        wManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        wChannel = wManager.initialize(this, mainLooper, null)



        wManager.removeGroup(wChannel,object:WifiP2pManager.ActionListener{
            override fun onSuccess() {

            }

            override fun onFailure(p0: Int) {

            }
        })
//        wManager.requestConnectionInfo(wChannel){
//            p0 -> if (p0.isGroupOwner){
//
//
//
//            } }
        val serverSocketThread = createServerSocketThread()
        if(serverSocketThread!=null) {
            Timber.i("serverSocketThread launch!")
            serverSocketThread.start()
        }






//        wChannel = wManager?.initialize(this, mainLooper, null)
//        wChannel?.also { channel ->
//            wReceiver = WiFiBroadcastReceiver(wManager, channel, this)
//        }


    }

    override fun onDestroy() {
        super.onDestroy()
        serverSocket?.close()
    }

    override fun onPause() {
        super.onPause()



//        wReceiver?.also { receiver ->
//            unregisterReceiver(receiver)
//        }

        unregisterReceiver(wReceiver)
    }

    override fun onResume() {
        super.onResume()
        wReceiver = WiFiBroadcastReceiver(wManager, wChannel, this)
        registerReceiver(wReceiver, intentFilter)
//        wReceiver?.also {
//                receiver ->
//            registerReceiver(receiver, intentFilter)
//        }





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
//                    wChannel?.also {
//                        channel -> wManager?.connect(channel,config,object :WifiP2pManager.ActionListener{
//                        override fun onSuccess() {
//                            Timber.i("connect success")
//                        }
//
//                        override fun onFailure(p0: Int) {
//                            Timber.i("connect fail")
//                        }
//                        })
//                    }


//                    Timber.i("connect operation,target = ${peers[index].deviceName}")
                }

                "sendMessageToServer" -> {


                    wManager.requestConnectionInfo(wChannel){
                            p0->
                        if(p0.isGroupOwner){
                            serverSend("message from sever!")
                        }else{
                            clientSend("message from client!")
                        }
                    }





//                    wManager.stopPeerDiscovery(wChannel,object :WifiP2pManager.ActionListener{
//                        override fun onSuccess() {
//                            Timber.i("stopPeerDiscovery onSuccess")
//                        }
//
//                        override fun onFailure(p0: Int) {
//                            Timber.i("stopPeerDiscovery onFailure")
//                        }
//
//                    })






//                    clientSendMessage(host = groupOwnerAddress,port = nServerPort)

                }

                "createServerSocketThread" -> {
//                    val serverSocketThread = createServerSocketThread()
//                    if(serverSocketThread!=null) {
//                        Timber.i("serverSocketThread launch!")
//                        serverSocketThread.start()
//                    }
                }
                else -> result.notImplemented()
            }
        }

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
                                    Timber.i("wifiP2pGroup = ${wifiP2pGroup.clientList}")
                                    Timber.i("isGroupOwner = ${p0.isGroupOwner}")
                                    groupOwnerAddress = p0.groupOwnerAddress.hostAddress
                                    if(!p0.isGroupOwner){
                                        val clientThread = createClientThread(host = groupOwnerAddress, port =nServerPort)
                                        clientThread!!.start()
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
//                    (activity.supportFragmentManager.findFragmentById(R.id.frag_list) as DeviceListFragment)
//                        .apply {
//                            updateThisDevice(
//                                intent.getParcelableExtra(
//                                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE) as WifiP2pDevice
//                            )
//                        }
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
