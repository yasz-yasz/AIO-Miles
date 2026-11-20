package com.aiomiles.app.data

import android.content.Context

class WalletStorage(context: Context) {
    private val prefs = context.getSharedPreferences("aio_miles_wallet", Context.MODE_PRIVATE)

    fun load(): MilesWallet {
        return MilesWallet(
            garudaMiles = prefs.getInt("gm", 0),
            krisFlyer = prefs.getInt("kf", 0),
            asiaMiles = prefs.getInt("am", 0)
        )
    }

    fun save(wallet: MilesWallet) {
        prefs.edit()
            .putInt("gm", wallet.garudaMiles)
            .putInt("kf", wallet.krisFlyer)
            .putInt("am", wallet.asiaMiles)
            .apply()
    }
}
