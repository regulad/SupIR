package xyz.regulad.makehex

class MakeHex {

    /**
     * A native method that is implemented by the 'makehex' native library,
     * which is packaged with this application.
     */
    external fun encodeIr(protocol: String, device: Int, subdevice: Int, function: Int): String?

    companion object {
        // Used to load the 'makehex' library on application startup.
        init {
            System.loadLibrary("makehex")
        }
    }
}
