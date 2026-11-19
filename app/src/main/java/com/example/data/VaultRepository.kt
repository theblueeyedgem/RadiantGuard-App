package com.example.data

import com.example.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.crypto.SecretKey

class VaultRepository(private val vaultDao: VaultDao) {

    // Reads from DB and decrypts on the fly
    val allDecryptedItems: Flow<List<DecryptedVaultItem>> = vaultDao.getAllItems().map { items ->
        items.map { item ->
            // Only decrypt if session is active
            if (CryptoManager.isSessionActive()) {
                try {
                    DecryptedVaultItem(
                        id = item.id,
                        title = CryptoManager.decrypt(item.titleEnc),
                        username = CryptoManager.decrypt(item.usernameEnc),
                        url = CryptoManager.decrypt(item.urlEnc),
                        password = CryptoManager.decrypt(item.passwordEnc),
                        notes = CryptoManager.decrypt(item.notesEnc),
                        totpSecret = CryptoManager.decrypt(item.totpSecretEnc),
                        isPinned = item.isPinned,
                        isLocked = item.isLocked,
                        lockPasswordHash = item.lockPasswordHash,
                        createdAt = item.createdAt
                    )
                } catch (e: Exception) {
                    // Fallback to empty if decryption fails (e.g., wrong key or corrupt data)
                    DecryptedVaultItem(
                        id = item.id,
                        title = "Decryption Error",
                        username = "", url = "", password = "", notes = "", totpSecret = "",
                        isPinned = item.isPinned,
                        isLocked = item.isLocked,
                        lockPasswordHash = item.lockPasswordHash,
                        createdAt = item.createdAt
                    )
                }
            } else {
                 DecryptedVaultItem(
                        id = item.id,
                        title = "Locked",
                        username = "", url = "", password = "", notes = "", totpSecret = "",
                        isPinned = item.isPinned,
                        isLocked = item.isLocked,
                        lockPasswordHash = item.lockPasswordHash,
                        createdAt = item.createdAt
                    )
            }
        }
    }

    suspend fun insertOrUpdate(decryptedItem: DecryptedVaultItem) {
        if (!CryptoManager.isSessionActive()) throw IllegalStateException("Cannot insert, vault is locked")
        // Preserve fields the UI does not carry on every save (createdAt, pin/lock state).
        val existing = if (decryptedItem.id != 0) vaultDao.getItemById(decryptedItem.id) else null
        val encryptedItem = VaultItem(
            id = decryptedItem.id,
            titleEnc = CryptoManager.encrypt(decryptedItem.title),
            usernameEnc = CryptoManager.encrypt(decryptedItem.username),
            urlEnc = CryptoManager.encrypt(decryptedItem.url),
            passwordEnc = CryptoManager.encrypt(decryptedItem.password),
            notesEnc = CryptoManager.encrypt(decryptedItem.notes),
            totpSecretEnc = CryptoManager.encrypt(decryptedItem.totpSecret),
            isPinned = decryptedItem.isPinned || (existing?.isPinned ?: false),
            isLocked = decryptedItem.isLocked || (existing?.isLocked ?: false),
            lockPasswordHash = decryptedItem.lockPasswordHash.ifEmpty { existing?.lockPasswordHash ?: "" },
            createdAt = if (decryptedItem.id == 0) System.currentTimeMillis() else (existing?.createdAt ?: decryptedItem.createdAt)
        )
        if (decryptedItem.id == 0) {
            vaultDao.insertItem(encryptedItem)
        } else {
            vaultDao.updateItem(encryptedItem)
        }
    }

    suspend fun deleteById(id: Int) = vaultDao.deleteItemById(id)

    suspend fun getRawItems(): List<VaultItem> = vaultDao.getAllItemsOnce()

    suspend fun togglePin(id: Int) {
        val item = vaultDao.getItemById(id) ?: return
        vaultDao.updateItem(item.copy(isPinned = !item.isPinned))
    }

    suspend fun setItemLock(id: Int, lockPasswordHash: String) {
        val item = vaultDao.getItemById(id) ?: return
        vaultDao.updateItem(item.copy(isLocked = true, lockPasswordHash = lockPasswordHash))
    }

    suspend fun removeItemLock(id: Int) {
        val item = vaultDao.getItemById(id) ?: return
        vaultDao.updateItem(item.copy(isLocked = false, lockPasswordHash = ""))
    }

    /**
     * Re-encrypts every vault row from [oldKey] to [newKey] (used by PIN change).
     * Decrypts ALL rows with the old key FIRST; only if every row decrypts
     * cleanly does it write anything back. Any failure throws and leaves the
     * database untouched, so a bad old PIN can never wipe the vault.
     */
    suspend fun reencryptAll(oldKey: SecretKey, newKey: SecretKey) {
        val rows = vaultDao.getAllItemsOnce()
        val reencrypted = rows.map { item ->
            item.copy(
                titleEnc = CryptoManager.encryptWithKey(newKey, CryptoManager.decryptWithKey(oldKey, item.titleEnc)),
                usernameEnc = CryptoManager.encryptWithKey(newKey, CryptoManager.decryptWithKey(oldKey, item.usernameEnc)),
                urlEnc = CryptoManager.encryptWithKey(newKey, CryptoManager.decryptWithKey(oldKey, item.urlEnc)),
                passwordEnc = CryptoManager.encryptWithKey(newKey, CryptoManager.decryptWithKey(oldKey, item.passwordEnc)),
                notesEnc = CryptoManager.encryptWithKey(newKey, CryptoManager.decryptWithKey(oldKey, item.notesEnc)),
                totpSecretEnc = CryptoManager.encryptWithKey(newKey, CryptoManager.decryptWithKey(oldKey, item.totpSecretEnc))
            )
        }
        reencrypted.forEach { vaultDao.updateItem(it) }
    }
}
