package com.liyobor.android_wifi_direct

import android.content.Context
import android.os.Looper
import android.widget.Toast
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

class WebSocketClient constructor(
    private val context: Context,
    private val streamerHandler: MainActivity.EventStreamHandler,
    host:String?, port:Int){

    private var clientSocket : Socket? = null
    private var messageToServer:String? = null


    init {
        startClientThread(host,port)
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
        streamerHandler.enterChat()
        Thread {
            try {

                clientSocket = Socket()
                clientSocket?.bind(null)
                clientSocket?.connect((InetSocketAddress(host, port)), 500)

                val clientInputStream = clientSocket?.getInputStream()
                val clientOutputStream= clientSocket?.getOutputStream()
                val bufferedReader = BufferedReader(InputStreamReader(clientInputStream))
//                clientOutputStream?.write("createClientThread".toByteArray())
//                clientOutputStream?.write("\r\n".toByteArray())
                Thread {
                    while(clientSocket!!.isConnected){
                        if(messageToServer!=null){
//                            Timber.i("messageToServer !=null send!")
                            clientOutputStream?.write(messageToServer!!.toByteArray())
                            clientOutputStream?.write("\r\n".toByteArray())
                            clientOutputStream?.flush()
                            messageToServer=null
                        }
                    }
                }.start()
                Looper.prepare()
                while (clientSocket!!.isConnected){


                    val stringLine: String? = bufferedReader.readLine()
                    if(stringLine!=null){

                        Toast.makeText(context,"receive message : $stringLine", Toast.LENGTH_LONG).show()
                        streamerHandler.onMessageReceived(stringLine)
                    }


                }


            } catch (e: IOException) {
                e.printStackTrace()
            }

        }.start()
    }
}