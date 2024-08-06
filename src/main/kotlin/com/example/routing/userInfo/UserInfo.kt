package com.example.routing.userInfo

import com.example.routing.userInfo.get.userInfoRoutingGET
import com.example.routing.userInfo.post.userInfoRoutingPOST
import io.ktor.server.routing.*

fun Route.userInfoRouting() {
    userInfoRoutingPOST()
    userInfoRoutingGET()
}
