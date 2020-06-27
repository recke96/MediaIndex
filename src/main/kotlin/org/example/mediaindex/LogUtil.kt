package org.example.mediaindex

import org.slf4j.Logger
import org.slf4j.event.Level

fun Logger.log(level: Level, message: () -> String) = this.makeLoggingEventBuilder(level).log(message)

fun Logger.debug(message: () -> String) = this.log(Level.DEBUG, message)
fun Logger.info(message: () -> String) = this.log(Level.INFO, message)