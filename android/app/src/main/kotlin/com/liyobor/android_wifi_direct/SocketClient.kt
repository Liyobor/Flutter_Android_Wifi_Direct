package com.liyobor.android_wifi_direct

import android.content.Context
import timber.log.Timber
import java.io.DataInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class SocketClient(context: Context, streamerHandler: MainActivity.EventStreamHandler,port:Int,host:String?) :
    MySocketN(context, streamerHandler, port,host) {


    override fun start() {
        startClientThread(host,port)
    }

    private fun startClientThread(host:String?,port:Int){
        if (host==null){
            Timber.i("host can not be null!")
            return
        }
        Timber.i("create clientThread")
        Thread {
            try {
                socket = Socket()
                socket?.bind(null)
                socket?.connect((InetSocketAddress(host, port)), 500)
                inputStream = socket?.getInputStream()!!
                outputStream= socket?.getOutputStream()!!
                dataInputStream = DataInputStream(inputStream)
                startMonitoringMessage()

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }


    private fun writeMessageToOutputStream(){
        outputStream.write(messageSending!!.toByteArray())
        outputStream.write("\r\n".toByteArray())
        outputStream.flush()
    }

    private fun startMonitoringMessage(){


        Thread {
            while(socket!!.isConnected && !socket!!.isClosed){

                if(messageSending!=null) {
                    writeMessageToOutputStream()
                    messageSending=null
                }

            }

        }.start()

        audioDataHandler.initializeFile()


        val floatList = mutableListOf<Float>()
        var receiveCount = 1
        var temp = 0
        Thread{
            while (socket!!.isConnected && !socket!!.isClosed){


                try{
                    val rcv = dataInputStream.read()


                    Timber.i("rcv = $rcv")


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
        }.start()


    }
}