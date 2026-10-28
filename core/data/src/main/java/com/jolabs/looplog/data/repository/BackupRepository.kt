package com.jolabs.looplog.data.repository

import com.jolabs.looplog.data.backup.BackupHeaderInfo

/** Outcome of importing a backup file. */
sealed interface BackupImportResult {
    /** Restore succeeded. [languageTag] (if present) must be applied by the caller. */
    data class Success(val habitCount: Int, val languageTag: String?) : BackupImportResult

    /** The bytes aren't a looplog backup at all. */
    data object NotABackupFile : BackupImportResult

    /** Wrong password, wrong app, or corrupted/tampered file. */
    data object WrongPasswordOrCorrupt : BackupImportResult

    /** The backup was written by a newer schema than this app understands. */
    data class IncompatibleVersion(val backupDbVersion: Int, val currentDbVersion: Int) : BackupImportResult

    /** Decrypted fine but restoring failed (malformed payload, DB error). */
    data object Failed : BackupImportResult
}

interface BackupRepository {
    /** Serializes + encrypts everything into a single file's bytes. */
    suspend fun export(password: String?, languageTag: String?): ByteArray

    /** Reads the clear header without decrypting, or null if [bytes] isn't a looplog backup. */
    fun readHeader(bytes: ByteArray): BackupHeaderInfo?

    /** Decrypts and, on success, atomically replaces all local data with the backup's contents. */
    suspend fun import(bytes: ByteArray, password: String?): BackupImportResult
}
