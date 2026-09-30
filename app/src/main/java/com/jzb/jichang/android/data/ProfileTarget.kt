package com.jzb.jichang.android.data

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/** Captured when an action starts; suspending must never retarget it to another profile. */
class ProfileTarget(val id: String) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ProfileTarget>
}

class RuleSnapshot(val rules: List<com.jzb.jichang.android.model.RoutingRule>) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<RuleSnapshot>
}
