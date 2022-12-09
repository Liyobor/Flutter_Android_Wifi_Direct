package com.liyobor.android_wifi_direct

import timber.log.Timber
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class SocketServer(
    private val streamerHandler: MainActivity.EventStreamHandler
) :MainActivity.MySocket{

    private var messageToClient:String? = null
    private var serverSocket: ServerSocket? = null
    private val nServerPort = 8888

    private lateinit var inputStream : InputStream
    private lateinit var outputStream :OutputStream
    private lateinit var dataInputStream : DataInputStream
    private lateinit var dataOutputStream : DataOutputStream

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
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
            dataInputStream = DataInputStream(inputStream)
            dataOutputStream = DataOutputStream(outputStream)
            Timber.i("socket.isConnected = ${socket.isConnected}")
            Thread{
                while(socket.isConnected){
                    if(messageToClient!=null){

                        dataOutputStream.writeUTF(messageToClient)
                        dataOutputStream.flush()
                        messageToClient = null
                    }
                }
            }.start()
            while (socket.isConnected){
                val stringLine = dataInputStream.readUTF()
                streamerHandler.onMessageReceived(stringLine)
            }

        }catch (e:Exception){
            e.printStackTrace()
        }

    }

    override fun start() {
        startServerThread()
    }

    override fun close() {
        if(serverSocket!=null){
            inputStream.close()
            outputStream.close()
            dataInputStream.close()
            dataOutputStream.close()
            serverSocket?.close()
        }
    }

    override fun send(message: String) {
        Timber.i("serverSend")
        messageToClient = message
    }
}