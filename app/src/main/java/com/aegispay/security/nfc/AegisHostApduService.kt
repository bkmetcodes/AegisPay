package com.aegispay.security.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.bekacodes.aegispay.data.AegisPayDao
import com.bekacodes.aegispay.data.AegisPayDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
open class AegisHostApduService : HostApduService() {

    @Inject
    lateinit var dao: AegisPayDao

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return STATUS_FAILED

        val isPpseSelect = isSelectPpseCommand(commandApdu)
        if (!isPpseSelect) {
            return STATUS_UNKNOWN_COMMAND
        }

        val activeCardIsFrozen = runCatching {
            runBlocking {
                val daoInstance = if (::dao.isInitialized) dao else AegisPayDatabase.getInstance(applicationContext).dao()
                val cards = daoInstance.getCardsSnapshot()
                cards.firstOrNull()?.isFrozen ?: false
            }
        }.getOrDefault(defaultValue = false)

        return if (activeCardIsFrozen) {
            STATUS_CARD_FROZEN
        } else {
            TOKENIZED_PPSE_RESPONSE + STATUS_SUCCESS
        }
    }

    override fun onDeactivated(reason: Int) {}

    private fun isSelectPpseCommand(apdu: ByteArray): Boolean {
        if (apdu.size < PPSE_AID.size) return false
        val apduHex = apdu.joinToString("") { "%02X".format(it) }
        val ppseHex = PPSE_AID.joinToString("") { "%02X".format(it) }
        val isSelectHeader = apdu.size >= 4 && apdu[0] == 0x00.toByte() && apdu[1] == 0xA4.toByte()
        return apduHex.contains(ppseHex) || isSelectHeader
    }

    companion object {
        // "2PAY.SYS.DDF01" in ASCII bytes
        val PPSE_AID = byteArrayOf(
            0x32, 0x50, 0x41, 0x59, 0x2E, 0x53, 0x59, 0x53, 0x2E, 0x44, 0x44, 0x46, 0x30, 0x31,
        )

        val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        val STATUS_CARD_FROZEN = byteArrayOf(0x69.toByte(), 0x85.toByte())
        val STATUS_FAILED = byteArrayOf(0x6F.toByte(), 0x00.toByte())
        val STATUS_UNKNOWN_COMMAND = byteArrayOf(0x6D.toByte(), 0x00.toByte())

        // Tokenized PPSE response with ISO 7816-4 status word 90 00
        val TOKENIZED_PPSE_RESPONSE = byteArrayOf(
            0x6F.toByte(), 0x23.toByte(),
            0x84.toByte(), 0x0E.toByte(),
            0x32, 0x50, 0x41, 0x59, 0x2E, 0x53, 0x59, 0x53, 0x2E, 0x44, 0x44, 0x46, 0x30, 0x31,
            0xA5.toByte(), 0x11.toByte(),
            0xBF.toByte(), 0x0C.toByte(), 0x0E.toByte(),
            0x61.toByte(), 0x0C.toByte(),
            0x4F.toByte(), 0x07.toByte(), 0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x04.toByte(), 0x10.toByte(), 0x10.toByte(),
            0x87.toByte(), 0x01.toByte(), 0x01.toByte(),
        )
    }
}
