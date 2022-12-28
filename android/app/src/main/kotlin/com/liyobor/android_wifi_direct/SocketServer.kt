package com.liyobor.android_wifi_direct

import android.content.Context
import timber.log.Timber
import java.io.DataInputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class SocketServer(context: Context, streamerHandler: MainActivity.EventStreamHandler, port: Int) :
    MySocket(context, streamerHandler,port) {

    private var nServerPort = port

    override fun setupWorkingThread(): Thread? {
        return try {
            Timber.i("startServerThread")
            serverSocket = ServerSocket(nServerPort)
            Timber.i("nServerPort = $nServerPort")
            Timber.i("Waiting for client connection…")
            val thread = Thread{
                waitForClient()
            }
            thread
        } catch (e: IOException) {
            e.printStackTrace()
            null
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
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
            dataInputStream = DataInputStream(inputStream)
            Timber.i("socket.isConnected = ${socket.isConnected}")
            startMonitoringMessage(socket.isConnected,socket.isClosed)

        }catch (e:Exception){
            e.printStackTrace()
        }

    }

    private fun writeMessageToOutputStream(){
        outputStream.write(messageSending!!.toByteArray())
        outputStream.write("\r\n".toByteArray())
        outputStream.flush()
    }

    private fun startMonitoringMessage(isConnected:Boolean,isClosed:Boolean){
        Thread{
            while(isConnected && !isClosed){
                if(messageSending!=null){
                    writeMessageToOutputStream()
                    messageSending = null
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


                if(adpcm.isEnable){
                    val leftFragment = (rcv shr 4)
                    val rightFragment = (rcv and 0xf)

                    floatList.add(adpcm.adpcm3Decode(leftFragment))
                    floatList.add(adpcm.adpcm3Decode(rightFragment))
                    if (floatList.size >= 3200) {
                        audioDataHandler.writeInTemp(floatList.toFloatArray())
                        floatList.clear()
                    }

                }else{
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
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }
}