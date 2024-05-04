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
package com.ashampoo.kim.output

@Suppress("MagicNumber")
internal class LittleEndianBinaryByteWriter(byteWriter: ByteWriter) : BinaryByteWriter(byteWriter) {

    override fun write2Bytes(value: Int) {
        write(0xFF and value)
        write(0xFF and (value shr 8))
    }

//    override fun write3Bytes(value: Int) {
//        write(0xFF and value)
//        write(0xFF and (value shr 8))
//        write(0xFF and (value shr 16))
//    }

    override fun write4Bytes(value: Int) {
        write(0xFF and value)
        write(0xFF and (value shr 8))
        write(0xFF and (value shr 16))
        write(0xFF and (value shr 24))
    }
}
