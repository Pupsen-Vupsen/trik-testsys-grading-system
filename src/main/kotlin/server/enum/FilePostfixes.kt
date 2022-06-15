package server.enum

enum class FilePostfixes(val text: String) {

    QRS(".qrs"),
    HASH_PIN_TXT("_hash_pin.txt"),
    TESTING("_testing"),
    INFO(".info")
}