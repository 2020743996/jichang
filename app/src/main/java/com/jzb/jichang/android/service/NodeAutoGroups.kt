package com.jzb.jichang.android.service

import com.jzb.jichang.android.model.ProxyNode

data class NodeRegion(val key: String, val title: String, val filter: String, val matcher: Regex)

object NodeAutoGroups {
    const val OTHER = "other"

    val regions = listOf(
        NodeRegion("hk", "香港", "(?i)香港|港|hong[ ._-]?kong|\\bhk\\b", Regex("香港|港|hong[ ._-]?kong|\\bhk\\b", RegexOption.IGNORE_CASE)),
        NodeRegion("tw", "台湾", "(?i)台湾|臺灣|台北|taiwan|taipei|\\btw\\b", Regex("台湾|臺灣|台北|taiwan|taipei|\\btw\\b", RegexOption.IGNORE_CASE)),
        NodeRegion("jp", "日本", "(?i)日本|东京|大阪|japan|tokyo|osaka|\\bjp\\b", Regex("日本|东京|大阪|japan|tokyo|osaka|\\bjp\\b", RegexOption.IGNORE_CASE)),
        NodeRegion("sg", "新加坡", "(?i)新加坡|狮城|singapore|\\bsg\\b", Regex("新加坡|狮城|singapore|\\bsg\\b", RegexOption.IGNORE_CASE)),
        NodeRegion("us", "美国", "(?i)美国|洛杉矶|西雅图|纽约|united states|america|\\busa?\\b", Regex("美国|洛杉矶|西雅图|纽约|united states|america|\\busa?\\b", RegexOption.IGNORE_CASE)),
        NodeRegion("kr", "韩国", "(?i)韩国|首尔|korea|seoul|\\bkr\\b", Regex("韩国|首尔|korea|seoul|\\bkr\\b", RegexOption.IGNORE_CASE)),
    )

    val allKeys: Set<String> = regions.map { it.key }.toSet() + OTHER

    fun classify(name: String): String = regions.firstOrNull { it.matcher.containsMatchIn(name) }?.key ?: OTHER

    fun title(key: String): String = regions.firstOrNull { it.key == key }?.title ?: "其他"
}
