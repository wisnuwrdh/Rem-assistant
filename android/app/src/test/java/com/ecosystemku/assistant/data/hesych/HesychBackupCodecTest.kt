package com.ecosystemku.assistant.data.hesych

import com.ecosystemku.assistant.data.local.HesychCustomFieldDto
import com.ecosystemku.assistant.data.local.HesychItemEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HesychBackupCodecTest {

    @Test
    fun masterBackupRoundTripsAndRejectsDifferentDek() {
        val dekA = HesychCrypto.importAesKey(ByteArray(32) { 10 })
        val dekB = HesychCrypto.importAesKey(ByteArray(32) { 20 })
        val row = HesychItemEntity(
            id = 1L,
            title = HesychCrypto.encryptWith(dekA, "GitHub"),
            username = HesychCrypto.encryptWith(dekA, "wisnu"),
            password = HesychCrypto.encryptWith(dekA, "super-secret"),
            notes = HesychCrypto.encryptWith(dekA, "akun dev"),
            category = HesychCrypto.encryptMetaValue(dekA, "work"),
            tags = HesychCrypto.encryptMetaValue(dekA, listOf("dev", "git"))
        )

        val jsonStr = HesychBackupCodec.exportMasterBackupJson(listOf(row), """{"v":2}""")
        assertFalse(HesychBackupCodec.bundleNeedsCustomPassword(jsonStr))

        val restored = HesychBackupCodec.parseImportRows(jsonStr, dekA)
        assertEquals(1, restored.size)
        assertEquals("GitHub", HesychCrypto.decryptWith(dekA, restored.first().title))

        var failed = false
        try {
            HesychBackupCodec.parseImportRows(jsonStr, dekB)
        } catch (_: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun customPasswordBackupReencryptsAcrossDifferentVaultKeys() {
        val dekSource = HesychCrypto.importAesKey(ByteArray(32) { 11 })
        val dekTarget = HesychCrypto.importAesKey(ByteArray(32) { 22 })
        val fieldsJson = Json.encodeToString(
            listOf(HesychCustomFieldDto(name = "PIN", value = HesychCrypto.encryptWith(dekSource, "123456"), type = "password"))
        )
        val row = HesychItemEntity(
            id = 5L,
            title = HesychCrypto.encryptWith(dekSource, "Bank"),
            username = HesychCrypto.encryptWith(dekSource, "user@bank"),
            password = HesychCrypto.encryptWith(dekSource, "bank-pass"),
            customFieldsJson = fieldsJson,
            category = HesychCrypto.encryptMetaValue(dekSource, "finance"),
            tags = HesychCrypto.encryptMetaValue(dekSource, listOf("bank"))
        )

        val customJson = HesychBackupCodec.exportCustomBackupJson(listOf(row), dekSource, "backup-pw-99")
        assertTrue(HesychBackupCodec.bundleNeedsCustomPassword(customJson))

        val imported = HesychBackupCodec.parseImportRows(customJson, dekTarget, "backup-pw-99")
        assertEquals(1, imported.size)
        val importedRow = imported.first()
        assertEquals("Bank", HesychCrypto.decryptWith(dekTarget, importedRow.title))
        assertEquals("bank-pass", HesychCrypto.decryptWith(dekTarget, importedRow.password))
        assertEquals("finance", HesychCrypto.decryptMetaValue(dekTarget, importedRow.category, "other"))
        assertEquals(listOf("bank"), HesychCrypto.decryptMetaValue(dekTarget, importedRow.tags, emptyList<String>()))

        val importedFields = Json.decodeFromString<List<HesychCustomFieldDto>>(importedRow.customFieldsJson)
        assertEquals("123456", HesychCrypto.decryptWith(dekTarget, importedFields.first().value))
    }

    @Test
    fun legacyMasterV3BackupWithSaltReencryptsIntoNewDek() {
        val salt = ByteArray(24) { (it + 3).toByte() }
        val legacyKey = HesychCrypto.derivePbkdf2Key("old-web-master", salt)
        val dekTarget = HesychCrypto.importAesKey(ByteArray(32) { 33 })

        val encTitle = HesychCrypto.encryptWith(legacyKey, "Discord")
        val encUser = HesychCrypto.encryptWith(legacyKey, "wisnu#0001")
        val encPass = HesychCrypto.encryptWith(legacyKey, "disc-pass-123")
        val encCat = HesychCrypto.encryptMetaValue(legacyKey, "social")!!
        val encTags = HesychCrypto.encryptMetaValue(legacyKey, listOf("chat"))!!

        val v3Json = """
            {
              "type": "vault-encrypted-backup",
              "pwMode": "master",
              "v": 3,
              "salt": "${HesychCrypto.toBase64(salt)}",
              "data": [
                {
                  "id": 1,
                  "title": "$encTitle",
                  "username": "$encUser",
                  "password": "$encPass",
                  "notes": "",
                  "totp_secret": "",
                  "custom_fields": [],
                  "color": 1,
                  "category": "$encCat",
                  "favorite": true,
                  "tags": "$encTags",
                  "breachStatus": null,
                  "breachCheckedAt": null,
                  "_metaV": 6
                }
              ]
            }
        """.trimIndent()

        var askedPassword = false
        try {
            HesychBackupCodec.parseImportRows(v3Json, dekTarget, null)
        } catch (ex: HesychBackupNeedsPasswordException) {
            askedPassword = ex.isMasterBackup
        }
        assertTrue(askedPassword)

        val imported = HesychBackupCodec.parseImportRows(v3Json, dekTarget, "old-web-master")
        assertEquals(1, imported.size)
        val row = imported.first()
        assertEquals("Discord", HesychCrypto.decryptWith(dekTarget, row.title))
        assertEquals("wisnu#0001", HesychCrypto.decryptWith(dekTarget, row.username))
        assertEquals("disc-pass-123", HesychCrypto.decryptWith(dekTarget, row.password))
        assertEquals("social", HesychCrypto.decryptMetaValue(dekTarget, row.category, "other"))
        assertEquals(listOf("chat"), HesychCrypto.decryptMetaValue(dekTarget, row.tags, emptyList<String>()))
    }
}
