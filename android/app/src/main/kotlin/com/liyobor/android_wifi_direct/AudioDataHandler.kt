package com.liyobor.android_wifi_direct

import android.content.Context
import android.os.Build
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.concurrent.schedule

class AudioDataHandler(private val context: Context,
                       private val streamerHandler: MainActivity.EventStreamHandler,) {
    private var fileTmp: File? = null
    private var outputStream: BufferedOutputStream? = null
    private var fileSize: Int = 0
    private var wavCache: ByteArray? = null
    private var uuidString = getUUID(context)

    companion object {
        const val UPLOAD_FILE_URL =
            "https://9nxqbm8t84.execute-api.ap-northeast-1.amazonaws.com/demo/upload"
    }

    fun initializeFile(){
        fileSize = 0
        outputStream?.close()
        if (fileTmp?.exists() == true) {
            fileTmp?.delete()
        }
        try {
            fileTmp = File.createTempFile("tmp_", ".raw")
            if (fileTmp?.exists() != true) return
            val output = FileOutputStream(fileTmp)
            outputStream = BufferedOutputStream(output)
            streamerHandler.onIsRecording(true)

//            System.out.println(outputStream.javaClass.output)
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }

    fun writeInTemp(audioData: FloatArray){

        try {
            val bytes = ByteArray(audioData.size * 4)
            ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asFloatBuffer().put(audioData)
            outputStream?.write(bytes)
            fileSize += audioData.size * 4
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stop(){
//        streamerHandler.onIsRecording(false)
        Timber.i("stop")
        Timber.i("isNetworkAvailable = ${isNetworkAvailable(context)}")
        outputStream?.close()

    }

    fun getWavConvertThread():Thread? {
        if (fileTmp?.exists() != true) {
            Timber.i("fileTmp not exist")
            return null
        }
        if (fileTmp!!.length() <= 100){
            Timber.i("fileTmp!!.length() <= 100")
            return null
        }

        return Thread {
            try {
//                val fileSize: Int = String.valueOf(wavFileTmp!!.length() / 1024).toInt()
                val raw = fileTmp!!.readBytes().copyOfRange(0, fileSize)
                wavCache = WavFileBuilder()
                    .setAudioFormat(WavFileBuilder.WAVE_FORMAT_IEEE_FLOAT)
                    .setSampleRate(16000)
                    .setBitsPerSample(WavFileBuilder.BITS_PER_SAMPLE_32)
                    .setNumChannels(WavFileBuilder.CHANNELS_MONO)
                    .setSubChunk1Size(WavFileBuilder.SUB_CHUNK_1_SIZE_PCM)
                    .build(raw)
//                if (isNetworkAvailable(context)) {
//                    uploadFile()
//                } else {
//                    deleteCache()
//                }
            } catch (e: FileNotFoundException) {
                Timber.i("file not found exception %s", e.localizedMessage)
            }
        }
    }

    fun uploadFile() {
        if (wavCache == null) {
            Timber.i("wav cache empty, skipped")
            return
        }
        uuidString = getUUID(context)
        // file name with : is not able to download from s3 on win pc
        val currentFormattedTime = getCurrentTime()
        val client = OkHttpClient()
        try {
            val body = wavCache!!.toRequestBody("audio/wav".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(
                    "$UPLOAD_FILE_URL?filename=${
                        Build.MODEL + "_WiFiTest/" + currentFormattedTime + "_" + uuidString.substring(
                            0,
                            8
                        )
                    }.wav"
                )
                .post(body).build()
            val response = client.newCall(request).execute()
            Timber.e("Response $response")
            response.close()
        } catch (e: Exception) {
            Timber.i("http time out $e")
        }

    }

    fun deleteCache() {
        Timer("deleteWavFile", true).schedule(500) {
            if (!fileTmp!!.delete()) {
                Timber.i("file not Deleted")
            }
            wavCache = null
        }
    }


}