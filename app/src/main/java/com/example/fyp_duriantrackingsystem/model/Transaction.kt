package com.example.fyp_duriantrackingsystem.model

import org.web3j.abi.datatypes.generated.Uint8

data class Transaction(
    val from: String,
    val to: String,
    val durianId: String,  // We map uint256 to String
    val timestamp: Long,
    val action: Action
)

enum class Action {
    Harvested,
    Transferred,
    UpdatedState,
    Removed;

    companion object {
        fun fromInt(value: Int): Action {
            return when (value) {
                0 -> Harvested
                1 -> Transferred
                2 -> UpdatedState
                3 -> Removed
                else -> throw IllegalArgumentException("Invalid action value: $value")
            }
        }
    }
}





