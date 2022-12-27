package net.sampro.yosef.models

open class ReceivedFileBase(
    var file_id: String? = null,
    var from: String? = null,
    var to: String? = null,
    var file_name: String? = null,
    var file_url: String? = null,
    var approval_status: String? = null,
    var approved_by: String? = null
)

data class ReceivedFileAdd(
    var received_at: MutableMap<String, String>? = null
) : ReceivedFileBase()

data class ReceivedFileGet(
    var received_at: Long? = null
) : ReceivedFileBase()































