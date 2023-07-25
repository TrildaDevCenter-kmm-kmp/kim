/*
 * Copyright 2023 Ashampoo GmbH & Co. KG
 * Copyright 2007-2023 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ashampoo.kim.format.raf

import com.ashampoo.kim.common.toSingleNumberHexes
import com.ashampoo.kim.format.ImageFormatMagicNumbers
import com.ashampoo.kim.format.PreviewExtractor
import com.ashampoo.kim.format.jpeg.JpegMetadataExtractor
import com.ashampoo.kim.input.ByteReader

object RafPreviewExtractor : PreviewExtractor {

    override fun extractPreviewImage(byteReader: ByteReader, length: Long): ByteArray? {

        val magicNumberBytes = byteReader.readBytes(ImageFormatMagicNumbers.raf.size).toList()

        /* Ensure it's actually an RAF. */
        require(magicNumberBytes == ImageFormatMagicNumbers.raf) {
            "RAF magic number mismatch: ${magicNumberBytes.toByteArray().toSingleNumberHexes()}"
        }

        RafMetadataExtractor.skipToJpegMagicBytes(byteReader)

        val bytes = mutableListOf<Byte>()

        bytes.addAll(ImageFormatMagicNumbers.jpeg)

        JpegMetadataExtractor.readSegmentBytesIntoList(byteReader, bytes)

        /*
         * Now we are in Start-of-Scan segment and need to read until FF D9 (EOI)
         */

        @Suppress("LoopWithTooManyJumpStatements")
        while (true) {

            val byte = byteReader.readByte() ?: break

            bytes.add(byte)

            /* Search the header and then break */
            if (bytes.size >= 2 &&
                bytes[bytes.lastIndex - 1] == JpegMetadataExtractor.SEGMENT_IDENTIFIER &&
                bytes[bytes.lastIndex - 0] == JpegMetadataExtractor.MARKER_END_OF_IMAGE
            ) break
        }

        return bytes.toByteArray()
    }
}
