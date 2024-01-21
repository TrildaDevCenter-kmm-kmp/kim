/*
 * Copyright 2024 Ashampoo GmbH & Co. KG
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
package com.ashampoo.kim.format.bmff.box

import com.ashampoo.kim.format.bmff.BoxType
import com.ashampoo.kim.format.tiff.TiffContents
import com.ashampoo.kim.format.tiff.TiffReader
import com.ashampoo.kim.input.ByteArrayByteReader

/**
 * JPEG XL Exif box
 */
class ExifBox(
    offset: Long,
    length: Long,
    payload: ByteArray
) : Box(BoxType.EXIF, offset, length, payload) {

    val version: Int

    val flags: ByteArray

    val exifBytes: ByteArray

    val tiffContents: TiffContents

    init {

        val byteReader = ByteArrayByteReader(payload)

        version = byteReader.readByteAsInt()

        flags = byteReader.readBytes("flags", 3)

        exifBytes = byteReader.readRemainingBytes()

        /* Directly parse here to ensure it's valid. */
        tiffContents = TiffReader.read(exifBytes)
    }
}
