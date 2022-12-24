package net.sampro.yosef.models

open class UserBase(
    var uid: String? = null,
    var name: String? = null,
    var email: String? = null,
    var type: String? = null
)

data class UserAdd(
    var Timestamp: MutableMap<String, String>? = null
) : UserBase()

data class UserGet(
    var Timestamp: Long? = null
) : UserBase()