package tools.android.apfmanager

import android.compact.impl.TaskPayload
import android.text.TextUtils
import tools.android.async2sync.Packet
import tools.android.async2sync.PacketFilter

class PacketIDFilter(private var packetId: String) : PacketFilter {

    override fun accept(packet: Packet<Any>?): Boolean {
        if (packet == null) {
            return false
        }
        if (!(packet.getContent() is TaskPayload)) {
            return false
        }
        val payloadContent = packet.getContent() as TaskPayload
        return if (TextUtils.isEmpty(payloadContent.ch)) {
            false
        } else payloadContent.ch == this.packetId
    }

}