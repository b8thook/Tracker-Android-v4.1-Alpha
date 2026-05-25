package co.neatfolk.triptracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AS-captured metadata from Grab's screens.
 * Stored separately from Trip and matched by startMs timestamp (±5 min window).
 * One TripMetadata record per trip — created when Screen 3 is captured,
 * updated when post-trip screen fires with actual fare.
 */
@Entity(tableName = "trip_metadata")
data class TripMetadata(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Timestamp of Screen 3 capture — used to match against Trip.startMs
    val capturedAtMs: Long = 0,

    // ── Screen 1 — abbreviated zone names (fallback) ─────────────────────────
    val pickupAbbrev: String = "",       // e.g. "Blk 136, Pebble Bay"
    val dropoffAbbrev: String = "",      // e.g. "Asian Civilisations Museum"
    val estimatedTripMin: Int = 0,       // "Total 28 min"

    // ── Screen 3 — full booking details ──────────────────────────────────────
    val passengerName: String = "",
    val pickupName: String = "",         // e.g. "B1 Lobby at Blk 17, Cape Royale"
    val pickupAddress: String = "",      // e.g. "17 Cove Way, Singapore 098205"
    val dropoffName: String = "",        // e.g. "Harry's Pub HarbourFront Centre"
    val dropoffAddress: String = "",     // e.g. "1 Maritime Square, Singapore 099253"
    val serviceType: String = "",        // e.g. "Kid | 6 seater (age 1-7)"
    val estimatedFare: Double? = null,   // from "You'll earn S$XX.XX"
    val paymentMethod: String = "",      // "GrabPay" or "Cash"
    val hasPromo: Boolean = false,       // amber "Promo" badge present
    val hasSurge: Boolean = false,       // ≈ surge icon next to fare

    // Special passenger requirements from Screen 1 tags
    val requiresCarSeat: Boolean = false,
    val requiresBoosterSeat: Boolean = false,
    val passengerCount: Int = 0,

    // ── Post-trip screen — actual earnings ────────────────────────────────────
    val actualFare: Double? = null,      // from "Your net earnings S$XX.XX"
    val fareConfirmed: Boolean = false,  // true once post-trip screen captured

    // Merge state
    val mergedToTripId: Long? = null     // Trip.id this metadata was matched to
)
