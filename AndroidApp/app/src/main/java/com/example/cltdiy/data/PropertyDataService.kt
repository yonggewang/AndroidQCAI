package com.example.cltdiy.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

// Data classes for parsing responses
data class Candidate(
    val address: String,
    val location: Location,
    val attributes: Map<String, String>?
)

data class Location(
    val x: Double,
    val y: Double
)

data class ParcelAttributes(
    val ownerName: String?,
    val ownName: String?,
    val name: String?,
    val legalOwner: String?,
    val pin: String?,
    val landUse: String?,
    val totalValue: Double?,
    val yearBuilt: Int?,
    val zoning: String?,
    val situsAddress: String?
) {
    fun formatString(): String {
        val sb = StringBuilder()
        val owner = ownerName ?: ownName ?: name ?: legalOwner
        if (owner != null) sb.append("- Owner: $owner\n")
        if (pin != null) sb.append("- Parcel ID (PIN): $pin\n")
        if (totalValue != null) sb.append("- Assessed Value: $totalValue\n")
        if (yearBuilt != null) sb.append("- Year Built: $yearBuilt\n")
        if (zoning != null) sb.append("- Zoning: $zoning\n")
        if (landUse != null) sb.append("- Land Use: $landUse\n")
        return sb.toString()
    }
}

class PropertyDataService {
    private val client = OkHttpClient()
    private val spatialReference = 4326

    suspend fun fetchPropertyData(address: String): String = withContext(Dispatchers.IO) {
        val candidate = geocodeAddress(address) ?: return@withContext "Could not geocode address specifically from Mecklenburg GIS."

        val x = candidate.location.x
        val y = candidate.location.y

        coroutineScope {
            val parcelDeferred = async { fetchParcelInfo(x, y) }
            val schoolsDeferred = async { fetchSchoolZone(x, y) }

            val parcel = parcelDeferred.await()
            val schools = schoolsDeferred.await()

            val sb = StringBuilder()
            sb.append("\n【OFFICIAL MECKLENBURG COUNTY GIS DATA】\n")
            sb.append("Source: charlottenc.gov\n")
            sb.append("Coordinates: $x, $y\n")

            if (parcel != null) {
                sb.append("\n[Property Details]\n")
                sb.append(parcel.formatString())
            } else {
                sb.append("\n[Property Details]\nNo official parcel record found. Hint: Check if the address is within Mecklenburg County.\n")
            }

            if (schools != null) {
                sb.append("\n[School & Zone Info]\n")
                val sortedKeys = schools.keys.sorted()
                for (key in sortedKeys) {
                    sb.append("- $key: ${schools[key]}\n")
                }
            }

            sb.append("\nINSTRUCTIONS: Use the above OFFICIAL values to populate the required fields (Owner, Value, Year, Schools). Do not hallucinate different values if these are present.\n")
            sb.toString()
        }
    }

    private fun geocodeAddress(street: String): Candidate? {
        val encodedStreet = URLEncoder.encode(street, "UTF-8")
        // Use SingleLine to support full address strings including City/Zip (e.g. Matthews, NC)
        val url = "https://gis.charlottenc.gov/arcgis/rest/services/Geocoding/AddressLocator/GeocodeServer/findAddressCandidates?SingleLine=$encodedStreet&outSR=$spatialReference&f=json"
        
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                val candidates = json.optJSONArray("candidates") ?: return null
                if (candidates.length() > 0) {
                    val first = candidates.getJSONObject(0)
                    val locObj = first.getJSONObject("location")
                    val location = Location(locObj.getDouble("x"), locObj.getDouble("y"))
                    val addressStr = first.optString("address")
                    return Candidate(addressStr, location, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun fetchParcelInfo(x: Double, y: Double): ParcelAttributes? {
        val url = "https://gis.charlottenc.gov/arcgis/rest/services/CountyData/Parcels/FeatureServer/0/query?geometry=$x,$y&geometryType=esriGeometryPoint&inSR=$spatialReference&spatialRel=esriSpatialRelIntersects&outFields=*&f=json"
        
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                val features = json.optJSONArray("features") ?: return null
                if (features.length() > 0) {
                    val attributes = features.getJSONObject(0).optJSONObject("attributes") ?: return null
                    
                    return ParcelAttributes(
                        ownerName = attributes.optString("OWNER_NAME").takeIf { it.isNotEmpty() },
                        ownName = attributes.optString("OWN_NAME").takeIf { it.isNotEmpty() },
                        name = attributes.optString("NAME").takeIf { it.isNotEmpty() },
                        legalOwner = attributes.optString("LEGAL_OWNER").takeIf { it.isNotEmpty() },
                        pin = attributes.optString("PIN").takeIf { it.isNotEmpty() },
                        landUse = attributes.optString("LANDUSE").takeIf { it.isNotEmpty() },
                        totalValue = attributes.optDouble("TOTAL_VALUE").takeIf { !it.isNaN() },
                        yearBuilt = attributes.optInt("YEAR_BUILT").takeIf { it > 0 },
                        zoning = attributes.optString("ZONING").takeIf { it.isNotEmpty() },
                        situsAddress = attributes.optString("SITUS_ADDRESS").takeIf { it.isNotEmpty() }
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun fetchSchoolZone(x: Double, y: Double): Map<String, Any>? {
        val url = "https://gis.charlottenc.gov/arcgis/rest/services/CountyData/SchoolAttendance/FeatureServer/0/query?geometry=$x,$y&geometryType=esriGeometryPoint&inSR=$spatialReference&spatialRel=esriSpatialRelIntersects&outFields=*&f=json"
        
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                val features = json.optJSONArray("features") ?: return null
                if (features.length() > 0) {
                    val attributes = features.getJSONObject(0).optJSONObject("attributes") ?: return null
                    val map = mutableMapOf<String, Any>()
                    val keys = attributes.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        map[key] = attributes.get(key)
                    }
                    return map
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
