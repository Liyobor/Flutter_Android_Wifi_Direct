package com.liyobor.android_wifi_direct

import android.content.Context
import io.flutter.plugin.common.EventChannel
import timber.log.Timber
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

abstract class MySocket{

    constructor(context: Context,streamerHandler: MainActivity.EventStreamHandler,port: Int){
        audioDataHandler = AudioDataHandler(context,streamerHandler)
        this.streamerHandler = streamerHandler
        this.port = port
    }

    constructor(context: Context, streamerHandler: MainActivity.EventStreamHandler, port:Int, host:String?) : this(context,streamerHandler,port) {
        this.host = host
    }


    val adpcm= Adpcm()
    var host:String? =null
    var port :Int


    var socket : Socket? = null
    var serverSocket :ServerSocket? = null
    var messageSending:String? = null

    var audioDataHandler:AudioDataHandler
    var streamerHandler:MainActivity.EventStreamHandler

    lateinit var inputStream : InputStream
    lateinit var outputStream: OutputStream
    lateinit var dataInputStream: DataInputStream

    open fun start(){
        resetState()
        setupWorkingThread()?.start()
    }


    private fun resetState(){
        adpcm.decodeStateReset()
        adpcm.encodeStateReset()
    }



    /** Initialization of socket or serverSocket need to be implement in subclass that inherited this abstract class.  */
    abstract fun setupWorkingThread():Thread?


    open fun close(){
        streamerHandler.onIsRecording(false)
        inputStream.close()
        outputStream.close()
        dataInputStream.close()
        socket?.close()
    }


    open fun sendMessage(message:String) {
        Timber.i("sendMessage")
        messageSending = message
    }

    open fun uploadAudioToAWS(){
        audioDataHandler.stop()
    }
}