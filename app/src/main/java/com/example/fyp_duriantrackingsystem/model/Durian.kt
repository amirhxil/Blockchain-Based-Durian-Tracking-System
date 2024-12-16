// File: model/Durian.kt

package com.example.fyp_duriantrackingsystem.model

import java.math.BigInteger

data class Durian(
    val batchCode: BigInteger,
    val durianType: String,
    val weightInGram: BigInteger,
    val currentOwner: String,
    val state: DurianState,  // Use enum here
    val farmLocation: String,
    val addedTimestamp: String,// New field for added timestamp
    val ownedTimestamp: String,
    val distributorName: String,
    val arrivedAtDistributor: String,
    val retailerName: String,
    val arrivedAtRetailer: String
)


enum class DurianState {
    Harvested,
    AtDistributor,
    AtRetailer,
    Sold,
    Spoiled
}
