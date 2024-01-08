/*
 * Copyright 2024 Ashampoo GmbH & Co. KG
 * Copyright 2002-2023 Drew Noakes and contributors
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
package com.ashampoo.kim.format.isobmff

import com.ashampoo.kim.format.isobmff.ISOBMFFConstants.BMFF_BYTE_ORDER
import com.ashampoo.kim.format.isobmff.boxes.Box
import com.ashampoo.kim.format.isobmff.boxes.FileTypeBox
import com.ashampoo.kim.format.isobmff.boxes.HandlerReferenceBox
import com.ashampoo.kim.format.isobmff.boxes.ItemInfoEntryBox
import com.ashampoo.kim.format.isobmff.boxes.ItemInformationBox
import com.ashampoo.kim.format.isobmff.boxes.ItemLocationBox
import com.ashampoo.kim.format.isobmff.boxes.MediaDataBox
import com.ashampoo.kim.format.isobmff.boxes.MetaBox
import com.ashampoo.kim.format.isobmff.boxes.PrimaryItemBox
import com.ashampoo.kim.input.PositionTrackingByteReader

/**
 * Reads ISOBMFF Boxes
 */
object BoxReader {

    /**
     * @param byteReader The reader as source for the bytes
     * @param stopAfterMetaBox If reading the file for metadata on the highest level we
     * want to stop reading after the meta box to prevent reading the whole mdat block in.
     * For iPhone HEIC this is possible, but Samsung HEIC has "meta" coming after "mdat"
     */
    fun readBoxes(
        byteReader: PositionTrackingByteReader,
        stopAfterMetaBox: Boolean = false
    ): List<Box> {

        val boxes = mutableListOf<Box>()

        while (true) {

            /*
             * Check if there are enough bytes for another box.
             * If so, we at least need the 8 header bytes.
             */
            if (byteReader.available < ISOBMFFConstants.BOX_HEADER_LENGTH)
                break

            val offset: Long = byteReader.position.toLong()

            /* Note: The length includes the 8 header bytes. */
            val length: Long =
                byteReader.read4BytesAsInt("length", BMFF_BYTE_ORDER).toLong()

            val type = BoxType.of(
                byteReader.readBytes("type", ISOBMFFConstants.TPYE_LENGTH)
            )

            val actualLength: Long = when (length) {

                /* A vaule of zero indicates that it's the last box. */
                0L -> byteReader.available

                /* A length of 1 indicates that we should read the next 8 bytes to get a long value. */
                1L -> byteReader.read8BytesAsLong("length", BMFF_BYTE_ORDER)

                /* Keep the length we already read. */
                else -> length
            }

            val nextBoxOffset = offset + actualLength

            val remainingBytesToReadInThisBox = (nextBoxOffset - byteReader.position).toInt()

            val bytes = byteReader.readBytes("data", remainingBytesToReadInThisBox)

            val box = when (type) {
                BoxType.FTYP -> FileTypeBox(offset, actualLength, bytes)
                BoxType.META -> MetaBox(offset, actualLength, bytes)
                BoxType.HDLR -> HandlerReferenceBox(offset, actualLength, bytes)
                BoxType.IINF -> ItemInformationBox(offset, actualLength, bytes)
                BoxType.INFE -> ItemInfoEntryBox(offset, actualLength, bytes)
                BoxType.ILOC -> ItemLocationBox(offset, actualLength, bytes)
                BoxType.PITM -> PrimaryItemBox(offset, actualLength, bytes)
                BoxType.MDAT -> MediaDataBox(offset, actualLength, bytes)
                else -> Box(offset, type, actualLength, bytes)
            }

            boxes.add(box)

            if (stopAfterMetaBox && type == BoxType.META)
                break
        }

        return boxes
    }
}
