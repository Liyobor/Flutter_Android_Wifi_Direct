package com.liyobor.android_wifi_direct

import timber.log.Timber
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket

class SocketClient(
    private val streamerHandler: MainActivity.EventStreamHandler,
    private val host: String?,
    private val port: Int):MainActivity.MySocket{

    private var clientSocket : Socket? = null
    private var messageToServer:String? = null

    private lateinit var inputStream:InputStream
    private lateinit var outputStream:OutputStream
    private lateinit var dataInputStream:DataInputStream
    private lateinit var dataOutputStream:DataOutputStream


    private fun startClientThread(host:String?,port:Int){
        if (host==null){
            Timber.i("host can not be null!")
            return
        }
        Timber.i("create clientThread")
        streamerHandler.enterChat()
        Thread {
            try {
                clientSocket = Socket()
                clientSocket?.bind(null)
                clientSocket?.connect((InetSocketAddress(host, port)), 500)
                inputStream = clientSocket?.getInputStream()!!
                outputStream= clientSocket?.getOutputStream()!!
                dataInputStream = DataInputStream(inputStream)
                dataOutputStream = DataOutputStream(outputStream)
                Thread {
                    while(clientSocket!!.isConnected){
                        if(messageToServer!=null){

                            dataOutputStream.writeUTF(messageToServer)
                            dataOutputStream.flush()
                            messageToServer=null
                        }
                    }
                }.start()
                while (clientSocket!!.isConnected){
                    val stringLine: String = dataInputStream.readUTF()
                    streamerHandler.onMessageReceived(stringLine)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun start() {
        startClientThread(host,port)
    }

    override fun close() {
        if(clientSocket!=null){
            inputStream.close()
            outputStream.close()
            dataInputStream.close()
            dataOutputStream.close()
            clientSocket?.close()
        }
    }

    override fun send(message:String) {
        Timber.i("clientSend")
        messageToServer = message
    }
}