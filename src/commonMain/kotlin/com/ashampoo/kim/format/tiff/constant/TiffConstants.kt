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
package com.ashampoo.kim.format.tiff.constant

import com.ashampoo.kim.common.ByteOrder

/**
 * Defines constants for internal elements from TIFF files and for allowing
 * applications to define parameters for reading and writing TIFF files.
 */
@Suppress("UnderscoresInNumericLiterals")
object TiffConstants {

    const val TIFF_VERSION: Int = 42

    /*
     * ExifTool defaults to big endian.
     * It's more natural to read.
     */
    val DEFAULT_TIFF_BYTE_ORDER = ByteOrder.BIG_ENDIAN

    const val TIFF_HEADER_SIZE = 8
    const val TIFF_DIRECTORY_HEADER_LENGTH = 2
    const val TIFF_DIRECTORY_FOOTER_LENGTH = 4
    const val TIFF_ENTRY_LENGTH = 12
    const val TIFF_ENTRY_MAX_VALUE_LENGTH = 4

    const val DIRECTORY_TYPE_ROOT = 0
    const val DIRECTORY_TYPE_SUB = 1
    const val DIRECTORY_TYPE_SUB0 = 1
    const val EXIF_SUB_IFD1 = 2
    const val EXIF_SUB_IFD2 = 3
    const val EXIF_SUB_IFD3 = 4

    const val TIFF_EXIF_IFD = -2
    const val TIFF_GPS = -3
    const val TIFF_INTEROP_IFD = -4

    const val DIRECTORY_TYPE_UNKNOWN = -1

    /* Artificial MakerNote directores */
    const val TIFF_MAKER_NOTE_CANON = -101
    const val TIFF_MAKER_NOTE_NIKON = -102

    /** Root directory */
    const val TIFF_IFD0 = 0

    /** Thumbnail directory */
    const val TIFF_IFD1 = 1

    const val TIFF_IFD2 = 2

    const val TIFF_IFD3 = 3

    const val DIRECTORY_TYPE_DIR_4 = 4

    const val FIELD_TYPE_BYTE_INDEX = 1
    const val FIELD_TYPE_ASCII_INDEX = 2
    const val FIELD_TYPE_SHORT_INDEX = 3
    const val FIELD_TYPE_LONG_INDEX = 4
    const val FIELD_TYPE_RATIONAL_INDEX = 5
    const val FIELD_TYPE_SBYTE_INDEX = 6
    const val FIELD_TYPE_UNDEFINED_INDEX = 7
    const val FIELD_TYPE_SSHORT_INDEX = 8
    const val FIELD_TYPE_SLONG_INDEX = 9
    const val FIELD_TYPE_SRATIONAL_INDEX = 10
    const val FIELD_TYPE_FLOAT_INDEX = 11
    const val FIELD_TYPE_DOUBLE_INDEX = 12
    const val FIELD_TYPE_IFD_INDEX = 13
}