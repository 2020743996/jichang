package com.jzb.jichang.android.service

import com.jzb.jichang.android.model.RoutingRule

/** Keys exist only for this editor session; even identical rules have different keys. */
data class RuleRow(val key: Long, val originalIndex: Int, val rule: RoutingRule)

class RuleOrderSession(initial: List<RoutingRule>) {
    private var nextKey = 0L
    var snapshot: List<RoutingRule> = initial
        private set
    var rows: List<RuleRow> = initial.mapIndexed { index, rule -> RuleRow(nextKey++, index, rule) }
        private set

    fun accept(rules: List<RoutingRule>, preferred: List<RuleRow> = rows): List<RuleRow> {
        val buckets = preferred.groupBy { it.rule }.mapValues { it.value.toMutableList() }
        rows = rules.mapIndexed { index, rule ->
            val old = buckets[rule]?.removeFirstOrNull()
            RuleRow(old?.key ?: nextKey++, index, rule)
        }
        snapshot = rules
        return rows
    }

    fun moved(order: List<RuleRow>, key: Long, destination: Int): List<RuleRow> {
        val from = order.indexOfFirst { it.key == key }
        if (from < 0 || order[from].rule.type.equals("MATCH", true)) return order
        val last = order.indexOfFirst { it.rule.type.equals("MATCH", true) }.let { if (it < 0) order.lastIndex else it - 1 }
        if (last < 0) return order
        val to = destination.coerceIn(0, last)
        if (from == to) return order
        return order.toMutableList().apply { add(to, removeAt(from)) }
    }
}
