package com.liyobor.android_wifi_direct

import android.content.Context
import android.os.Looper
import android.widget.Toast
import timber.log.Timber
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket


class SocketServer constructor(
    context: Context,
    private val streamerHandler: MainActivity.EventStreamHandler): MainActivity.MySocket {

    private var messageToClient:String? = null
    private var serverSocket: ServerSocket? = null
    private val nServerPort = 8888

    private val audioDataHandler = AudioDataHandler(context)

    private lateinit var serverInputStream :InputStream
    private lateinit var serverOutputStream :OutputStream
    private lateinit var dataInputStream:DataInputStream


    override fun start() {
        startServerThread()
    }

    override fun close(){
        serverInputStream.close()
        serverOutputStream.close()
        dataInputStream.close()
        serverSocket?.close()
    }

    override fun sendMessage(message: String) {
        messageToClient = message
    }

    override fun uploadAudioToAWS() {
        audioDataHandler.stop()
    }


    private fun startServerThread(){
        try {
            Timber.i("startServerThread")
            serverSocket = ServerSocket(nServerPort)
            Timber.i("Waiting for client connection…")
            Thread{
                waitForClient()
            }.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun waitForClient(){

        try {
            Timber.i("waitForClient")
            val socket = serverSocket!!.accept()
            Timber.i("accept!")
            streamerHandler.enterChat()
            newClient(socket)
        }catch (e:Exception){
            e.printStackTrace()
        }

    }

    private fun newClient(socket: Socket){
        try{
            serverInputStream = socket.getInputStream()
            serverOutputStream = socket.getOutputStream()
//            val bufferedReader = BufferedReader(InputStreamReader(serverInputStream))
            dataInputStream = DataInputStream(serverInputStream)
//            var stringLine:String?
            Timber.i("socket.isConnected = ${socket.isConnected}")
            startMonitoringMessage(socket.isConnected,socket.isClosed)
//            Thread{
//                while(socket.isConnected && !socket.isClosed){
//                    if(messageToClient!=null){
//                        serverOutputStream.write(messageToClient!!.toByteArray())
//                        serverOutputStream.write("\r\n".toByteArray())
//                        serverOutputStream.flush()
//                        messageToClient = null
//                    }
//                }
//            }.start()
//            Looper.prepare()
//            while (socket.isConnected && !socket.isClosed){
//                stringLine = bufferedReader.readLine()
//                if(stringLine!=null){
//                    Toast.makeText(context,"receive message : $stringLine", Toast.LENGTH_LONG).show()
//                    streamerHandler.onMessageReceived(stringLine)
//                }
//            }

        }catch (e:Exception){
            e.printStackTrace()
        }

    }

    private fun writeMessageToOutputStream(){
        serverOutputStream.write(messageToClient!!.toByteArray())
        serverOutputStream.write("\r\n".toByteArray())
        serverOutputStream.flush()
    }

    private fun startMonitoringMessage(isConnected:Boolean,isClosed:Boolean){
        Thread{
            while(isConnected && !isClosed){
                if(messageToClient!=null){
                    writeMessageToOutputStream()
                    messageToClient = null
                }
            }
        }.start()

        audioDataHandler.initializeFile()
        val floatList = mutableListOf<Float>()
        var receiveCount = 1
        var temp = 0
        while (isConnected && !isClosed){
            try {
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
        }
    }
}