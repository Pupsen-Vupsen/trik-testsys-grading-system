package server.enum

enum class TRIKLinux(command: String) {

    TWO_D_MODEL("./TRIKStudio/bin/2D-model -platform offscreen -s 10 -b -r"),
    PATCHER("./TRIKStudio/bin/patcher -f")
}