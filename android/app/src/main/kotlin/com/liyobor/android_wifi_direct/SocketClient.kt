package com.liyobor.android_wifi_direct

import android.content.Context
import timber.log.Timber
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class SocketClient constructor(
    private val context: Context,
    private val streamerHandler: MainActivity.EventStreamHandler,
    host:String?, port:Int){

    private var clientSocket : Socket? = null
    private var messageToServer:String? = null
    private val audioDataHandler = AudioDataHandler(context)


    private lateinit var clientInputStream :InputStream
    private lateinit var clientOutputStream:OutputStream
    private lateinit var dataInputStream:DataInputStream

    init {
        startClientThread(host,port)
    }

    fun close(){
        clientInputStream.close()
        clientOutputStream.close()
        dataInputStream.close()
        clientSocket?.close()
    }

    fun clientSend(message:String){
        Timber.i("clientSend")
        messageToServer = message
    }

    private fun startClientThread(host:String?,port:Int){
        if (host==null){
            Timber.i("host can not be null!")
            return
        }
        Timber.i("create clientThread")
//        streamerHandler.enterChat()
        Thread {
            try {
                clientSocket = Socket()
                clientSocket?.bind(null)
                clientSocket?.connect((InetSocketAddress(host, port)), 500)
                clientInputStream = clientSocket?.getInputStream()!!
                clientOutputStream= clientSocket?.getOutputStream()!!
//                val bufferedReader = BufferedReader(InputStreamReader(clientInputStream))
                dataInputStream = DataInputStream(clientInputStream)
//                Thread {
//                    while(clientSocket!!.isConnected && !clientSocket!!.isClosed){
//                        messageSend()
////                        if(messageToServer!=null){
////                            clientOutputStream.write(messageToServer!!.toByteArray())
////                            clientOutputStream.write("\r\n".toByteArray())
////                            clientOutputStream.flush()
////                            messageToServer=null
////                        }
//                    }
//                }.start()
//                Looper.prepare()
                startMonitoringMessage()
//                while (clientSocket!!.isConnected && !clientSocket!!.isClosed){
//                    val rcv = dataInputStream.read()
//                    Timber.i("rcv = $rcv")
//                    val stringLine: String? = bufferedReader.readLine()

//                    Toast.makeText(context,"receive message : $stringLine", Toast.LENGTH_LONG).show()
//                    streamerHandler.onMessageReceived(rcv.toString())
//                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }


    private fun writeMessageToOutputStream(){
        clientOutputStream.write(messageToServer!!.toByteArray())
        clientOutputStream.write("\r\n".toByteArray())
        clientOutputStream.flush()
    }

    private fun startMonitoringMessage(){


        Thread {
            while(clientSocket!!.isConnected && !clientSocket!!.isClosed){

                if(messageToServer!=null) {
                    writeMessageToOutputStream()
                    messageToServer=null
                }
            }
        }.start()

        audioDataHandler.initializeFile()


        val floatList = mutableListOf<Float>()
        var receiveCount = 1
        var temp = 0
        while (clientSocket!!.isConnected && !clientSocket!!.isClosed){


//            read/handle message in this scope

//            val rcv = dataInputStream.read()
            try{
                val rcv = dataInputStream.read()
                if (receiveCount % 2 == 1) {
                    temp = rcv
                } else {
                    temp += (rcv.shl(8))
                    val short = temp.toShort()
                    Timber.i("temp = $short")
                    floatList.add((short / 32767.0).toFloat())
                    temp = 0
                }
                if (floatList.size >= 3200) {
                    audioDataHandler.writeInTemp(floatList.toFloatArray())
                    floatList.clear()
                }
                receiveCount += 1
            }catch (e:Exception){
                e.printStackTrace()
            }

//                    val stringLine: String? = bufferedReader.readLine()

//                    Toast.makeText(context,"receive message : $stringLine", Toast.LENGTH_LONG).show()
//                    streamerHandler.onMessageReceived(rcv.toString())
        }

    }

    fun upload(){
        audioDataHandler.stop()
    }
}