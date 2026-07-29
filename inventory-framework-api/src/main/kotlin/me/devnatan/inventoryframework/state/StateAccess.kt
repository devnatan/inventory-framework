@file:JvmSynthetic

package me.devnatan.inventoryframework.state

import me.devnatan.inventoryframework.state.timer.TimerState
import kotlin.time.Duration

private const val SECONDS_TO_TICKS_DIV = 20L

/**
 * Creates a new timer state.
 *
 * @param interval The initial interval the timer will run at.
 * @return A new timer state.
 * @see StateAccess.timerState
 */
public fun StateAccess<*, *>.timerState(interval: Duration): TimerState = timerState(interval.inWholeSeconds * SECONDS_TO_TICKS_DIV)
