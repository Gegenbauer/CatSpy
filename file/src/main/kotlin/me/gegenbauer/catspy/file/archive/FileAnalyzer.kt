package me.gegenbauer.catspy.file.archive

sealed class FileType {

    object TextFile : FileType()
    object NonTextFile : FileType()
    object Directory : FileType()
    class CompressedFile(val compressionType: CompressionType) : FileType()

    companion object {
        /**
         * read 512 bytes from the file and check if it contains any non-ASCII characters
         */
        fun isTextFile(byteArray: ByteArray): Boolean {
            val length = byteArray.size.coerceAtMost(512)
            return byteArray.take(length).all { it.toInt() in 32..126 || it.toInt() in 9..13 }
        }

        fun isCompressedFile(byteArray: ByteArray): Boolean {
            return CompressionType.identifyCompression(byteArray) !is CompressionType.Unknown
        }
    }
}

sealed class CompressionType {
    open fun isOfType(byteArray: ByteArray): Boolean {
        return true
    }

    object Gzip : CompressionType() {
        override fun isOfType(byteArray: ByteArray): Boolean {
            return byteArray.size >= 2 && byteArray[0] == 0x1F.toByte() && byteArray[1] == 0x8B.toByte()
        }
    }

    object Zip : CompressionType() {
        override fun isOfType(byteArray: ByteArray): Boolean {
            return byteArray.size >= 4 && byteArray[0] == 0x50.toByte() && byteArray[1] == 0x4B.toByte() &&
                    (byteArray[2] == 0x03.toByte() || byteArray[2] == 0x05.toByte() || byteArray[2] == 0x07.toByte())
        }
    }

    object Tar : CompressionType() {
        /**
         * Check for "ustar"
         */
        override fun isOfType(byteArray: ByteArray): Boolean {
            if (byteArray.size < 265) return false
            return byteArray[257] == 0x75.toByte() && byteArray[258] == 0x73.toByte() &&
                    byteArray[259] == 0x74.toByte() && byteArray[260] == 0x61.toByte() &&
                    byteArray[261] == 0x72.toByte()
        }
    }

    object SevenZip : CompressionType() {
        /**
         * Check for "7z"
         */
        override fun isOfType(byteArray: ByteArray): Boolean {
            if (byteArray.size < 6) return false
            return byteArray[0] == 0x37.toByte() && byteArray[1] == 0x7A.toByte() &&
                    byteArray[2] == 0xBC.toByte() && byteArray[3] == 0xAF.toByte() &&
                    byteArray[4] == 0x27.toByte() && byteArray[5] == 0x1C.toByte()
        }
    }

    object Rar : CompressionType() {
        /**
         * Check for "Rar!" - RAR archive header for version 2.9
         * Check for "Rar!\x1A\x07\x00" - RAR archive header for version 5.0
         */
        override fun isOfType(byteArray: ByteArray): Boolean {
            if (byteArray.size < 7) return false
            if (byteArray[0] == 0x52.toByte() && byteArray[1] == 0x61.toByte() &&
                byteArray[2] == 0x72.toByte() && byteArray[3] == 0x21.toByte() &&
                byteArray[4] == 0x1A.toByte() && byteArray[5] == 0x07.toByte() &&
                byteArray[6] == 0x00.toByte()
            ) {
                return true
            }
            if (byteArray[0] == 0x52.toByte() && byteArray[1] == 0x61.toByte() &&
                byteArray[2] == 0x72.toByte() && byteArray[3] == 0x21.toByte()
            ) {
                return true
            }
            return false
        }
    }

    object Bzip2 : CompressionType() {
        override fun isOfType(byteArray: ByteArray): Boolean {
            return byteArray.size >= 3 && byteArray[0] == 0x42.toByte() && byteArray[1] == 0x5A.toByte() && byteArray[2] == 0x68.toByte()
        }
    }

    object Xz : CompressionType() {
        override fun isOfType(byteArray: ByteArray): Boolean {
            return byteArray.size >= 6 && byteArray[0] == 0xFD.toByte() && byteArray[1] == 0x37.toByte() &&
                    byteArray[2] == 0x7A.toByte() && byteArray[3] == 0x58.toByte() &&
                    byteArray[4] == 0x5A.toByte() && byteArray[5] == 0x00.toByte()
        }
    }

    object Unknown : CompressionType()

    companion object {
        /**
         * read 8 bytes from the file and identify the compression type
         */
        fun identifyCompression(byteArray: ByteArray): CompressionType {
            val header = byteArray.take(300).toByteArray()
            return when {
                Gzip.isOfType(header) -> Gzip
                Zip.isOfType(header) -> Zip
                Tar.isOfType(header) -> Tar
                SevenZip.isOfType(header) -> SevenZip
                Rar.isOfType(header) -> Rar
                Bzip2.isOfType(header) -> Bzip2
                Xz.isOfType(header) -> Xz
                else -> Unknown
            }
        }
    }
}


