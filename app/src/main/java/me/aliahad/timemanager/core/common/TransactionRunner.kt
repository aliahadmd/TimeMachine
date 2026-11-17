package me.aliahad.timemanager.core.common

import androidx.room.withTransaction
import me.aliahad.timemanager.data.TimerDatabase

interface TransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}

class RoomTransactionRunner(private val database: TimerDatabase) : TransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return database.withTransaction { block() }
    }
}

object ImmediateTransactionRunner : TransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
}
