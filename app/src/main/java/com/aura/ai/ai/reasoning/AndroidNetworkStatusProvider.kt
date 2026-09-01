package com.aura.ai.ai.reasoning

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.aura.ai.core.reasoning.context.ConnectivityState
import com.aura.ai.core.reasoning.context.NetworkStatusProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The real [NetworkStatusProvider], reading the device's actual active network capabilities —
 *  "offline awareness" made concrete. Requires only `ACCESS_NETWORK_STATE`, a normal
 *  (install-time-granted) permission, not a runtime one. */
class AndroidNetworkStatusProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : NetworkStatusProvider {
        override fun currentState(): ConnectivityState {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return ConnectivityState.Unknown

            val network = connectivityManager.activeNetwork ?: return ConnectivityState.Offline
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectivityState.Offline

            val isOnline =
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            return if (isOnline) ConnectivityState.Online else ConnectivityState.Offline
        }
    }
