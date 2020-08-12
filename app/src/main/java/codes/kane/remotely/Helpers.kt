package codes.kane.remotely

import java.util.*

/// Formats a byte array into a human readable hex string, each byte separated by a space.
val ByteArray.hexString: String get() = joinToString(" ") { String.format("%02X", it) }

/// Returns true if the int contains the provided flag, using bitwise-and.
fun Int.hasFlag(flag: Int) = flag and this == flag