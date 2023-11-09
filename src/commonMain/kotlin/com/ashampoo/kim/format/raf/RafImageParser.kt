/*
 * Copyright 2023 Ashampoo GmbH & Co. KG
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

import com.ashampoo.kim.common.ImageReadException
import com.ashampoo.kim.common.toSingleNumberHexes
import com.ashampoo.kim.common.tryWithImageReadException
import com.ashampoo.kim.format.ImageFormatMagicNumbers
import com.ashampoo.kim.format.ImageMetadata
import com.ashampoo.kim.format.ImageParser
import com.ashampoo.kim.format.jpeg.JpegImageParser
import com.ashampoo.kim.input.ByteReader
import com.ashampoo.kim.input.PrePendingByteReader
import com.ashampoo.kim.model.ImageFormat

object RafImageParser : ImageParser {

    /**
     * The RAF file contains a JPEG with EXIF metadata.
     * We just have to find it and read the data from there it.
     */
    @Throws(ImageReadException::class)
    @Suppress("ComplexCondition", "LoopWithTooManyJumpStatements")
    override fun parseMetadata(byteReader: ByteReader): ImageMetadata =
        tryWithImageReadException {

            val magicNumberBytes = byteReader.readBytes(ImageFormatMagicNumbers.raf.size).toList()

            /* Ensure it's actually an RAF. */
            require(magicNumberBytes == ImageFormatMagicNumbers.raf) {
                "RAF magic number mismatch: ${magicNumberBytes.toByteArray().toSingleNumberHexes()}"
            }

            RafMetadataExtractor.skipToJpegMagicBytes(byteReader)

            /* Create a new reader, prepending the jpegMagicNumbers, and read the contained JPEG. */
            val newReader = PrePendingByteReader(
                delegate = byteReader,
                prependedBytes = ImageFormatMagicNumbers.jpeg
            )

            return@tryWithImageReadException JpegImageParser
                .parseMetadata(newReader)
                .copy(imageFormat = ImageFormat.RAF)
        }
}
