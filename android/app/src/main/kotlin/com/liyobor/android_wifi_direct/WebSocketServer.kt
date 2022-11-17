package com.liyobor.android_wifi_direct

import android.content.Context
import android.os.Looper
import android.widget.Toast
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

class WebSocketServer constructor(private val context: Context) {

    private var messageToClient:String? = null
    private var serverSocket: ServerSocket? = null
    private val nServerPort = 8888



    init {
        startServerThread()
    }

    fun serverSend(message:String){
        Timber.i("serverSend")
        messageToClient = message
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
            addNewClient(socket)
        }catch (e:Exception){
            e.printStackTrace()
        }


    }

    private fun addNewClient(socket: Socket){
        try{

            val serverInputStream = socket.getInputStream()
            val serverOutputStream = socket.getOutputStream()
            val bufferedReader = BufferedReader(InputStreamReader(serverInputStream))
            var stringLine:String?
            Timber.i("socket.isConnected = ${socket.isConnected}")
            Thread{
                while(socket.isConnected){
                    if(messageToClient!=null){
                        serverOutputStream.write(messageToClient!!.toByteArray())
                        serverOutputStream.write("\r\n".toByteArray())
                        serverOutputStream.flush()
                        messageToClient = null
                    }
                }
            }.start()
            Looper.prepare()
            while (socket.isConnected){

                stringLine = bufferedReader.readLine()
                if(stringLine!=null){

                    Toast.makeText(context,"receive message : $stringLine", Toast.LENGTH_LONG).show()

                }




            }

        }catch (e:Exception){
            e.printStackTrace()
        }

    }
}