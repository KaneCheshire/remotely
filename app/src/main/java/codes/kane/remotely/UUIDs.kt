package codes.kane.remotely

import java.util.*

object UUIDs {
    object Services {
        // Controls things like lights, beeps and whether the remote is connected
        val general = UUID.fromString("F4C4772C-0056-11E6-8D22-5E5517507C66")
        // Has info like throttle and trigger position
        val throttle = UUID.fromString("AFC05DA0-0CD4-11E6-A148-3E1D05DEFE78")
        // Used for OTA updates
        val ota = UUID.fromString("00001016-D102-11E1-9B23-00025B00A5A5")
    }
}