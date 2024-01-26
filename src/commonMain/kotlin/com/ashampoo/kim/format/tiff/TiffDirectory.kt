/*
 * Copyright 2024 Ashampoo GmbH & Co. KG
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
package com.ashampoo.kim.format.tiff

import com.ashampoo.kim.common.ByteOrder
import com.ashampoo.kim.common.ImageReadException
import com.ashampoo.kim.common.ImageWriteException
import com.ashampoo.kim.common.toInt
import com.ashampoo.kim.common.toInts
import com.ashampoo.kim.format.tiff.constant.ExifTag
import com.ashampoo.kim.format.tiff.constant.TiffConstants
import com.ashampoo.kim.format.tiff.constant.TiffDirectoryType
import com.ashampoo.kim.format.tiff.constant.TiffTag
import com.ashampoo.kim.format.tiff.taginfo.TagInfo
import com.ashampoo.kim.format.tiff.taginfo.TagInfoBytes
import com.ashampoo.kim.format.tiff.taginfo.TagInfoGpsText
import com.ashampoo.kim.format.tiff.taginfo.TagInfoLong
import com.ashampoo.kim.format.tiff.taginfo.TagInfoLongs
import com.ashampoo.kim.format.tiff.write.TiffOutputDirectory
import com.ashampoo.kim.format.tiff.write.TiffOutputField
import com.ashampoo.kim.model.TiffOrientation

/**
 * Provides methods and elements for accessing an Image File Directory (IFD)
 * from a TIFF file. In the TIFF specification, the IFD is the main container
 * for individual images or sets of metadata. While not all Directories contain
 * images, images are always stored in a Directory.
 */
class TiffDirectory(
    val type: Int,
    val entries: List<TiffField>,
    offset: Int,
    val nextDirectoryOffset: Int,
    val byteOrder: ByteOrder
) : TiffElement(
    debugDescription = "Directory " + description(type) + " @ $offset",
    offset = offset,
    length = TiffConstants.TIFF_DIRECTORY_HEADER_LENGTH + entries.size *
        TiffConstants.TIFF_ENTRY_LENGTH + TiffConstants.TIFF_DIRECTORY_FOOTER_LENGTH
) {

    var jpegImageDataElement: JpegImageDataElement? = null

    fun getDirectoryEntries(): List<TiffField> = entries

    fun hasJpegImageData(): Boolean =
        null != findField(TiffTag.TIFF_TAG_JPEG_INTERCHANGE_FORMAT)

    fun findField(tag: TagInfo): TiffField? {
        return findField(
            tag = tag,
            failIfMissing = false
        )
    }

    fun findField(tag: TagInfo, failIfMissing: Boolean = false): TiffField? {

        for (field in entries)
            if (field.tag == tag.tag)
                return field

        if (failIfMissing)
            throw ImageReadException("Missing expected field: " + tag.tagFormatted)

        return null
    }

    fun getFieldValue(tag: TagInfoBytes, mustExist: Boolean): ByteArray? {

        val field = findField(tag)

        if (field == null) {

            if (mustExist)
                throw ImageReadException("Required field ${tag.name} is missing")

            return null
        }

        return field.valueBytes
    }

    @Suppress("ThrowsCount")
    fun getFieldValue(tag: TagInfoLong): Int? {

        val field = findField(tag) ?: return null

        if (tag.fieldType != field.fieldType)
            throw ImageReadException("Required field ${tag.name} has incorrect type ${field.fieldType.name}")

        if (field.count != 1)
            throw ImageReadException("Field ${tag.name} has wrong count ${field.count}")

        return field.valueBytes.toInt(field.byteOrder)
    }

    @Suppress("ThrowsCount")
    fun getFieldValue(tag: TagInfoLongs): IntArray {

        val field = findField(tag)
            ?: throw ImageReadException("Required field ${tag.name} is missing")

        if (tag.fieldType != field.fieldType)
            throw ImageReadException("Required field ${tag.name} has incorrect type ${field.fieldType.name}")

        return field.valueBytes.toInts(field.byteOrder)
    }

    fun getJpegRawImageDataElement(): ImageDataElement {

        val jpegInterchangeFormat = findField(TiffTag.TIFF_TAG_JPEG_INTERCHANGE_FORMAT)
        val jpegInterchangeFormatLength = findField(TiffTag.TIFF_TAG_JPEG_INTERCHANGE_FORMAT_LENGTH)

        if (jpegInterchangeFormat != null && jpegInterchangeFormatLength != null) {

            val offset = jpegInterchangeFormat.toIntArray()[0]
            val byteCount = jpegInterchangeFormatLength.toIntArray()[0]

            return ImageDataElement(offset, byteCount)
        }

        throw ImageReadException("Couldn't find image data.")
    }

    fun createOutputDirectory(byteOrder: ByteOrder): TiffOutputDirectory {

        /*
         * Prevent attempts to add MakerNote directories.
         */
        @Suppress("MagicNumber")
        check(type > -100) {
            "Can't create OutputDirectory for artifical MakerNote directory."
        }

        try {

            val outputDirectory = TiffOutputDirectory(type, byteOrder)

            @Suppress("LoopWithTooManyJumpStatements")
            for (entry in entries) {

                /* Don't add double entries. */
                if (outputDirectory.findField(entry.tag) != null)
                    continue

                /* Skip known offsets. */
                if (entry.tagInfo?.isOffset == true)
                    continue

                val tagInfo = entry.tagInfo
                val fieldType = entry.fieldType
                var value = entry.value

                /*
                 * Automatic correction: Trim certain values like "Copyright"
                 * that come with huge amount of empty spaces and wasting space this way.
                 */
                if (value is String && tagsToTrim.contains(tagInfo)) {

                    value = value.replace("\u0000", "").trim()

                    /* Skip fields that only had whitespaces in it. */
                    if (value.isEmpty())
                        continue
                }

                val bytes = if (tagInfo is TagInfoGpsText)
                    tagInfo.encodeValue(value)
                else
                    fieldType.writeData(value, byteOrder)

                val count = bytes.size / fieldType.size

                val outputField = TiffOutputField(entry.tag, fieldType, count, bytes)

                outputField.sortHint = entry.sortHint

                outputDirectory.add(outputField)
            }

            /*
             * Check if the root directory has an orientation flag and
             * add this per default it is missing. If it is present we
             * can update the orientation easily the next time we need
             * to touch the file.
             */
            if (type == TiffDirectoryType.TIFF_DIRECTORY_IFD0.typeId) {

                val orientationField = outputDirectory.findField(TiffTag.TIFF_TAG_ORIENTATION)

                if (orientationField == null)
                    outputDirectory.add(
                        tagInfo = TiffTag.TIFF_TAG_ORIENTATION,
                        value = TiffOrientation.STANDARD.value.toShort()
                    )
            }

            outputDirectory.setJpegImageData(jpegImageDataElement)

            return outputDirectory

        } catch (ex: ImageReadException) {
            throw ImageWriteException(ex.message, ex)
        }
    }

    override fun toString(): String {

        val sb = StringBuilder()

        sb.appendLine("---- $debugDescription ----")

        for (entry in entries)
            sb.appendLine(entry)

        return sb.toString()
    }

    companion object {

        private val tagsToTrim = setOf(
            TiffTag.TIFF_TAG_COPYRIGHT,
            TiffTag.TIFF_TAG_ARTIST,
            ExifTag.EXIF_TAG_USER_COMMENT
        )

        @kotlin.jvm.JvmStatic
        fun description(type: Int): String {
            return when (type) {
                TiffConstants.DIRECTORY_TYPE_UNKNOWN -> "Unknown"
                TiffConstants.DIRECTORY_TYPE_ROOT -> "IFD0"
                TiffConstants.DIRECTORY_TYPE_SUB -> "SubIFD"
                TiffConstants.EXIF_SUB_IFD1 -> "SubIFD1"
                TiffConstants.EXIF_SUB_IFD2 -> "SubIFD2"
                TiffConstants.EXIF_SUB_IFD3 -> "SubIFD3"
                // TiffConstants.DIRECTORY_TYPE_THUMBNAIL -> "Thumbnail"
                TiffConstants.TIFF_EXIF_IFD -> "ExifIFD"
                TiffConstants.TIFF_GPS -> "GPS"
                TiffConstants.TIFF_INTEROP_IFD -> "InteropIFD"
                TiffConstants.TIFF_MAKER_NOTE_CANON -> "MakerNoteCanon"
                TiffConstants.TIFF_MAKER_NOTE_NIKON -> "MakerNoteNikon"
                else -> "Bad Type"
            }
        }

        /*
         * Note: Keep in sync with TiffTags.getTag()
         */
        @Suppress("UnnecessaryParentheses")
        fun findTiffField(directories: List<TiffDirectory>, tagInfo: TagInfo): TiffField? =
            directories.firstOrNull { directory ->
                directory.type == tagInfo.directoryType?.typeId ||
                    (tagInfo.directoryType?.isImageDirectory == true && directory.type >= 0) ||
                    (tagInfo.directoryType?.isImageDirectory == false && directory.type < 0)
            }?.findField(tagInfo)
    }
}
