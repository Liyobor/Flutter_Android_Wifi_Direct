package com.liyobor.android_wifi_direct

class Adpcm {
    private var encodeState = State()
    private var decodeState = State()

    private val indexTable3 = listOf(
        -1, -1, -1, -1, 2, 4, 6, 8,
        -1, -1, -1, -1, 2, 4, 6, 8
    )

    private val stepsizeTable3 = listOf(
        7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
        50, 55, 60, 66, 73, 80, 88, 97, 107, 118, 130, 143, 157, 173, 190, 209, 230,
        253, 279, 307, 337, 371, 408, 449, 494, 544, 598, 658, 724, 796, 876, 963,
        1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024,
        3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493,
        10442, 11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086,
        29794, 32767
    )


    init {
        this.encodeState = State()
        this.decodeState = State()
    }


    fun encodeStateReset(){
        encodeState.reset()
    }
    fun decodeStateReset(){
        decodeState.reset()
    }

    fun adpcm3Decode(delta:Int): Float {


        var valpred = decodeState.valprev

        var stateIndex = decodeState.index
        var step = stepsizeTable3[stateIndex]
        var vpdiff = step shr 3

        stateIndex += indexTable3[delta]
        if(stateIndex <0) {
            stateIndex = 0
        }
        else if(stateIndex > 88) {
            stateIndex = 88
        }

        if((delta and 4) > 0) {
            vpdiff += step
        }
        if((delta and 2) > 0) {
            vpdiff += step shr 1
        }
        if((delta and 1) > 0) {
            vpdiff += step shr 2
        }

        if(delta and 8 > 0){
            valpred -= vpdiff
            if (valpred < -32768){
                valpred = -32768
            }
        }else{
            valpred += vpdiff
            if(valpred >32767){
                valpred = 32767
            }
        }
        step = stepsizeTable3[stateIndex]
        decodeState.valprev = valpred
        decodeState.index = stateIndex


//        return valpred
        return if(valpred <0){
            valpred/32768f
        }else if(valpred > 0){
            valpred/32767f
        }else{
            valpred.toFloat()
        }
    }



    fun adpcm3Encode(inData:Byte): Int {
        var valpred:Int = encodeState.valprev
        var index:Int = encodeState.index
        var step:Int = stepsizeTable3[index]
        var diff:Int = inData - valpred
        var delta:Int
        if(diff<0){
            delta = 8
            diff = -diff
        }else{
            delta = 0
        }
        var vpdiff:Int = step shr 3

        if(diff>=step){
            delta = delta or 4
            diff -= step
            vpdiff += step
        }

        step = step shr 1
        if(diff>=step){
            delta = delta or 2
            diff -= step
            vpdiff += step
        }

        step = step shr 1
        if(diff>=step){
            delta = delta or 1
            vpdiff += step
        }

        if(delta and 8 > 0){
            valpred -= vpdiff
            if(valpred<-32768)
                valpred-=32768
        }
        else
        {
            valpred+=vpdiff
            if(valpred>32767)
                valpred=32767
        }
        index +=indexTable3[delta.toInt()]
        if(index<0) index = 0
        else if (index>88) index =88
        step = stepsizeTable3[index]
        encodeState.valprev = valpred
        encodeState.index = index
        return delta
    }




    class State {
        var valprev = 0
        var index = 0

        fun reset(){
            this.valprev=0
            this.index = 0
        }
    }
}




