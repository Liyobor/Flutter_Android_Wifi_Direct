package com.liyobor.android_wifi_direct

import java.util.*

class WavFileBuilder {


    companion object {

        /**
         * The wave format consist of two sub-chunks, the first one is "fmt " and the second is "data".
         * The "fmt " sub-chunk describes the format of the sound information in the "data" sub-chunk.
         * The "data" sub-chunk indicates the size of the sound information and contains the raw sound data.
         * http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/WAVE.html
         */
        const val SAMPLE_RATE_8000 = 16000
        const val BITS_PER_SAMPLE_32 = 32
        const val BITS_PER_SAMPLE_16 = 16
        const val BITS_PER_SAMPLE_8 = 8
        const val CHANNELS_MONO = 1
        const val CHANNELS_STEREO = 2
        const val SUB_CHUNK_1_SIZE_PCM = 16
        const val WAVE_FORMAT_PCM = 1
        const val WAVE_FORMAT_IEEE_FLOAT = 3
        //        0x0001	WAVE_FORMAT_PCM	PCM
//        0x0003	WAVE_FORMAT_IEEE_FLOAT	IEEE float
//        0x0006	WAVE_FORMAT_ALAW	8-bit ITU-T G.711 A-law
//        0x0007	WAVE_FORMAT_MULAW	8-bit ITU-T G.711 µ-law
//        0xFFFE	WAVE_FORMAT_EXTENSIBLE	Determined by SubFormat
        const val HEADER_SIZE = 44
    }


    private var channels = -1
    private var bitsPerSample = -1
    private var sampleRate = -1
    private val header: ByteArray = ByteArray(HEADER_SIZE)

    private fun setConstants() {

        /** chunkId - contains the letters "RIFF" in ASCII form  */

        this.header[0] = 'R'.code.toByte()
        this.header[1] = 'I'.code.toByte()
        this.header[2] = 'F'.code.toByte()
        this.header[3] = 'F'.code.toByte()

        /** format - contains the letters "WAVE"  */

        this.header[8] = 'W'.code.toByte()
        this.header[9] = 'A'.code.toByte()
        this.header[10] = 'V'.code.toByte()
        this.header[11] = 'E'.code.toByte()

        /** subchunk1Id - contains the letters "fmt "  */

        this.header[12] = 'f'.code.toByte()
        this.header[13] = 'm'.code.toByte()
        this.header[14] = 't'.code.toByte()
        this.header[15] = ' '.code.toByte()

        /** subchunk2Id - contains the letters "data"  */

        this.header[36] = 'd'.code.toByte()
        this.header[37] = 'a'.code.toByte()
        this.header[38] = 't'.code.toByte()
        this.header[39] = 'a'.code.toByte()
    }

    /**
     * 36 + SubChunk2Size, or more precisely:
     * 4 + (8 + SubChunk1Size) + (8 + SubChunk2Size)
     * This is the size of the rest of the chunk
     * following this number.  This is the size of the
     * entire file in bytes minus 8 bytes for the
     * two fields not included in this count: ChunkID and ChunkSize.
     */

    private fun setChunkSize(samplesCount: Int) {
        val size = 36 + samplesCount
        this.header[4] = (size and 0xFF).toByte()
        this.header[5] = (size shr 8 and 0xFF).toByte()
        this.header[6] = (size shr 16 and 0xFF).toByte()
        this.header[7] = (size shr 24 and 0xFF).toByte()
    }

    /** 16 for PCM. This is the size of the rest of the Subchunk which follows this number.  */

    fun setSubChunk1Size(size: Int): WavFileBuilder {
        this.header[16] = (size and 0xFF).toByte()
        this.header[17] = (size shr 8 and 0xFF).toByte()
        this.header[18] = (size shr 16 and 0xFF).toByte()
        this.header[19] = (size shr 24 and 0xFF).toByte()
        return this
    }

    /** PCM = 1 (i.e. Linear quantization) Values other than 1 indicate some form of compression.  */

    fun setAudioFormat(format: Int): WavFileBuilder {
        this.header[20] = (format and 0xFF).toByte()
        this.header[21] = (format shr 8 and 0xFF).toByte()
        return this
    }

    /** Number of channels. Mono = 1, Stereo = 2, etc.  */

    fun setNumChannels(channels: Int): WavFileBuilder {

        this.channels = channels

        this.header[22] = (channels and 0xFF).toByte()
        this.header[23] = (channels shr 8 and 0xFF).toByte()

        return this
    }

    /** Sample rate 8000, 44100, etc.  */

    fun setSampleRate(sampleRate: Int): WavFileBuilder {

        this.sampleRate = sampleRate

        this.header[24] = (sampleRate and 0xFF).toByte()
        this.header[25] = (sampleRate shr 8 and 0xFF).toByte()
        this.header[26] = (sampleRate shr 16 and 0xFF).toByte()
        this.header[27] = (sampleRate shr 24 and 0xFF).toByte()

        return this
    }

    /** sampleRate * numChannels * bitsPerSample / 8  */

    private fun setByteRate() {

        if (sampleRate == -1) throw IllegalArgumentException("The sample rate is not specified")

        if (channels == -1) throw IllegalArgumentException("The number of channels is not specified")

        if (bitsPerSample == -1) throw IllegalArgumentException("The bits per a sample is not specified")

        val byteRate = sampleRate * channels * bitsPerSample / 8

        this.header[28] = (byteRate and 0xFF).toByte()
        this.header[29] = (byteRate shr 8 and 0xFF).toByte()
        this.header[30] = (byteRate shr 16 and 0xFF).toByte()
        this.header[31] = (byteRate shr 24 and 0xFF).toByte()
    }

    /**
     * numChannels * bitsPerSample / 8
     * The number of bytes for one sample including
     * all channels. I wonder what happens when
     * this number isn't an integer?
     */

    private fun setBlockAlign() {

        if (channels == -1) throw IllegalArgumentException("The number of channels is not specified")

        if (bitsPerSample == -1) throw IllegalArgumentException("The bits per a sample is not specified")

        val byteCount = channels * bitsPerSample / 8

        this.header[32] = (byteCount and 0xFF).toByte()
        this.header[33] = (byteCount shr 8 and 0xFF).toByte()
    }

    /** 8 bits = 8, 16 bits = 16, etc.  */

    fun setBitsPerSample(bitsPerSample: Int): WavFileBuilder {

        this.bitsPerSample = bitsPerSample

        this.header[34] = (bitsPerSample and 0xFF).toByte()
        this.header[35] = (bitsPerSample shr 8 and 0xFF).toByte()

        return this
    }

    /**
     * NumSamples * NumChannels * BitsPerSample / 8
     * This is the number of bytes in the data.
     * You can also think of this as the size
     * of the read of the subchunk following this number
     */

    private fun setSubChunk2Size(size: Int) {

        this.header[40] = (size and 0xFF).toByte()
        this.header[41] = (size shr 8 and 0xFF).toByte()
        this.header[42] = (size shr 16 and 0xFF).toByte()
        this.header[43] = (size shr 24 and 0xFF).toByte()
    }

    /** returns only a header of the wav file   */

    private fun buildHeader(samplesCount: Int): ByteArray {

        setConstants()

        setByteRate()

        setBlockAlign()

        setSubChunk2Size(samplesCount)

        setChunkSize(samplesCount)

        return header
    }

    /** returns a completed wav file (header + audio data)  */

    fun build(data: ByteArray?): ByteArray? {

        if (data != null) {

            val dataLength = data.size

            buildHeader(dataLength)

            val wavBytes = ByteArray(dataLength + HEADER_SIZE)

            System.arraycopy(header, 0, wavBytes, 0, HEADER_SIZE)

            System.arraycopy(data, 0, wavBytes, HEADER_SIZE, dataLength)

            clear()

            return wavBytes
        }

        return null
    }

    /** clears all the variables  */

    private fun clear() {

        Arrays.fill(header, 0.toByte())

        this.channels = -1
        this.bitsPerSample = -1
        this.sampleRate = -1
    }
}