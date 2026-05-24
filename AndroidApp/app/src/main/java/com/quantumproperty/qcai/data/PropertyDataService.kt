package com.quantumproperty.qcai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.CookieJar
import org.json.JSONObject
import org.json.JSONArray
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

data class PropertyAttributes(
    var pid: String = "",
    var ownerName: String = "None Found",
    var lastSalePrice: Double = 0.0,
    var lastSaleDate: String = "N/A",
    var deedBook: String = "N/A",
    var deedPage: String = "N/A",
    var assessedValue: Double = 0.0,
    var landValue: Double = 0.0,
    var buildingValue: Double = 0.0,
    var taxAmount: Double = 0.0,
    var yearBuilt: Int = 0,
    var squareFeet: Int = 0,
    var bedrooms: Int? = null,
    var bathrooms: Double? = null,
    var zoning: String = "N/A"
)

// External owner footprint lookup services

object OpenAlexAPI {
    private val client = OkHttpClient()
    
    suspend fun searchScholarProfiles(name: String): String = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || cleanName.lowercase() == "none" || cleanName.lowercase() == "n/a" || cleanName.lowercase() == "none found") {
            return@withContext "No public footprint profiles located"
        }
        
        try {
            val encoded = URLEncoder.encode(cleanName, "UTF-8")
            val url = "https://api.openalex.org/authors?search=$encoded"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext "No public footprint profiles located"
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results") ?: return@withContext "No public footprint profiles located"
                    if (results.length() > 0) {
                        val sb = java.lang.StringBuilder()
                        val limit = minOf(results.length(), 3)
                        for (i in 0 until limit) {
                            val r = results.getJSONObject(i)
                            val displayName = r.optString("display_name", cleanName)
                            val worksCount = r.optInt("works_count", 0)
                            val citedByCount = r.optInt("cited_by_count", 0)
                            var instName = "Unknown Institution"
                            val insts = r.optJSONArray("last_known_institutions")
                            if (insts != null && insts.length() > 0) {
                                instName = insts.getJSONObject(0).optString("display_name", "Unknown Institution")
                            }
                            sb.append("- $displayName affiliated with $instName (Works: $worksCount, Citations: $citedByCount)\n")
                        }
                        return@withContext sb.toString().trim()
                    }
                }
            }
        } catch (e: Exception) {
            println("OpenAlex API Error: ${e.message}")
        }
        return@withContext "No public footprint profiles located"
    }
}

object JudyRecordsAPI {
    private val client = OkHttpClient()
    
    suspend fun searchCivilRecords(name: String): String = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || cleanName.lowercase() == "none" || cleanName.lowercase() == "n/a" || cleanName.lowercase() == "none found") {
            return@withContext "No public court records located"
        }
        
        try {
            val encoded = URLEncoder.encode(cleanName, "UTF-8")
            val url = "https://api.judyrecords.com/v1/records?q=$encoded"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext "No public court records located"
                    val json = JSONObject(body)
                    val records = json.optJSONArray("records") ?: return@withContext "No public court records located"
                    if (records.length() > 0) {
                        val sb = java.lang.StringBuilder()
                        val limit = minOf(records.length(), 3)
                        for (i in 0 until limit) {
                            val r = records.getJSONObject(i)
                            val caseTitle = r.optString("title", "Civil Case")
                            val court = r.optString("court", "State Court")
                            val date = r.optString("date", "")
                            sb.append("- $caseTitle in $court ($date)\n")
                        }
                        return@withContext sb.toString().trim()
                    }
                }
            }
        } catch (e: Exception) {
            println("JudyRecords API Error: ${e.message}")
        }
        return@withContext "No public court records located"
    }
}

// Result struct for property data fetching
data class PropertyDataResult(
    val formattedData: String,
    val hasOfficialGISData: Boolean,   // True if Mecklenburg County parcel data was found
    val coordinates: Pair<Double, Double>?, // (x/lon, y/lat)
    val promptPayload: String?         // The custom structured prompt for Gemini
)

class InMemoryCookieJar : okhttp3.CookieJar {
    private val cookieStore = mutableMapOf<String, List<okhttp3.Cookie>>()

    override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
        cookieStore[url.host] = cookies
    }

    override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }
}

class PropertyDataService {
    private val client = okhttp3.OkHttpClient.Builder()
        .cookieJar(InMemoryCookieJar())
        .build()
    private val spatialReference = 4326

    suspend fun fetchPropertyData(address: String): PropertyDataResult = withContext(Dispatchers.IO) {
        // Ensure address has city context for geocoding (Charlotte, NC default)
        val addrLower = address.lowercase()
        val normalizedAddress = if (
            addrLower.contains("charlotte") || addrLower.contains(", nc") ||
            addrLower.contains("matthews") || addrLower.contains("mint hill") ||
            addrLower.contains("huntersville") || addrLower.contains("cornelius") ||
            addrLower.contains("davidson") || addrLower.contains("pineville") ||
            addrLower.contains("mooresville") || addrLower.contains("concord")
        ) address else "$address, Charlotte, NC"
        
        // Strategy 1: Try US Census Geocoder first for Address Standardization
        var candidate = geocodeAddressCensus(normalizedAddress)
        var geocodeSource = "US Census Geocoder"

        // Strategy 2: Try Mecklenburg County GIS geocoder as fallback
        if (candidate == null) {
            println("PropertyDataService: Census geocode failed, trying Mecklenburg GIS...")
            candidate = geocodeAddressGIS(normalizedAddress)
            geocodeSource = "Mecklenburg County GIS"
        }

        // Strategy 3: Try Nominatim (OpenStreetMap) as final fallback
        if (candidate == null) {
            println("PropertyDataService: Mecklenburg GIS geocode failed, trying Nominatim fallback...")
            candidate = geocodeAddressNominatim(normalizedAddress)
            geocodeSource = "OpenStreetMap/Nominatim"
        }

        // If all geocoding strategies fail, return descriptive error
        if (candidate == null) {
            return@withContext PropertyDataResult(
                formattedData = "Could not geocode the address \"$address\" using any available geocoding service (Census, Mecklenburg GIS, OpenStreetMap).",
                hasOfficialGISData = false,
                coordinates = null,
                promptPayload = null
            )
        }

        val x = candidate.location.x
        val y = candidate.location.y

        coroutineScope {
            val spatialestDeferred = async { fetchSpatialestData(candidate.address, x, y) }
            val schoolsDeferred = async { fetchSchoolZone(x, y) }

            val spatialestAttrs = spatialestDeferred.await()
            val schools = schoolsDeferred.await()

            var property = PropertyAttributes()
            var hasOfficialGISData = false
            var geocodeSourceString = geocodeSource

            if (spatialestAttrs != null) {
                property = spatialestAttrs
                hasOfficialGISData = true
                geocodeSourceString = "Mecklenburg Property System (Spatialest)"
            } else {
                // Fallback to ArcGIS parcels boundaries layer
                val rawParcelAttrs = fetchParcelInfo(x, y)
                if (rawParcelAttrs != null) {
                    property = parsePropertyAttributes(rawParcelAttrs)
                    hasOfficialGISData = property.pid.isNotEmpty() || property.ownerName != "None Found"
                }
            }

            // Perform parallel background footprint searches using Owner Name
            val civilCourtSummaryDeferred = async { JudyRecordsAPI.searchCivilRecords(property.ownerName) }
            val academicAffiliationsDeferred = async { OpenAlexAPI.searchScholarProfiles(property.ownerName) }

            val courtRecords = civilCourtSummaryDeferred.await()
            val professionalData = academicAffiliationsDeferred.await()

            val footprintString = if (courtRecords == "No public court records located" && professionalData == "No public footprint profiles located") {
                "No public footprint profiles located"
            } else {
                "Associated Court/Civil Records:\n$courtRecords\n\nProfessional/Academic Associations:\n$professionalData"
            }

            val sb = StringBuilder()

            if (hasOfficialGISData) {
                sb.append("\n【OFFICIAL MECKLENBURG COUNTY GIS DATA】\n")
                sb.append("Source: charlottenc.gov / Spatialest\n")
                sb.append("Geocode Source: $geocodeSourceString\n")
                sb.append("Coordinates: $x, $y\n")

                sb.append("\n[Property Details]\n")
                sb.append("- Owner: ${property.ownerName}\n")
                sb.append("- Parcel ID (PIN): ${property.pid}\n")
                if (property.assessedValue > 0) sb.append("- Assessed Value: ${property.assessedValue}\n")
                if (property.landValue > 0) sb.append("- Land Value: ${property.landValue}\n")
                if (property.buildingValue > 0) sb.append("- Building Value: ${property.buildingValue}\n")
                if (property.yearBuilt > 0) sb.append("- Year Built: ${property.yearBuilt}\n")
                if (property.squareFeet > 0) sb.append("- Total Area: ${property.squareFeet} sq ft\n")
                if (property.bedrooms != null) sb.append("- Bedrooms: ${property.bedrooms}\n")
                if (property.bathrooms != null) sb.append("- Bathrooms: ${property.bathrooms}\n")
                if (property.zoning.isNotEmpty() && property.zoning != "N/A") sb.append("- Zoning/Land Use: ${property.zoning}\n")

                if (schools != null) {
                    sb.append("\n[School & Zone Info]\n")
                    val sortedKeys = schools.keys.sorted()
                    for (key in sortedKeys) {
                        sb.append("- $key: ${schools[key]}\n")
                    }
                }

                sb.append("\nINSTRUCTIONS: Use the above OFFICIAL values to populate the required fields. Do not hallucinate different values if these are present.\n")
            } else {
                sb.append("\n【GEOCODING RESULT - NO OFFICIAL GIS DATA AVAILABLE】\n")
                sb.append("Geocode Source: $geocodeSourceString\n")
                sb.append("Resolved Address: ${candidate.address}\n")
                sb.append("Coordinates: $x, $y\n")
                sb.append("\nNote: This property is NOT within the Mecklenburg County GIS database. No official parcel records are available.\n")
            }

            // Format values for prompt
            val salePriceString = if (property.lastSalePrice > 0) {
                String.format(java.util.Locale.US, "$%.2f", property.lastSalePrice)
            } else {
                "No historical transaction price recorded in county registry"
            }
            val taxAmountString = String.format(java.util.Locale.US, "$%.2f", property.taxAmount)

            val landValueString = if (property.landValue > 0) String.format(java.util.Locale.US, "$%.2f", property.landValue) else "N/A"
            val buildingValueString = if (property.buildingValue > 0) String.format(java.util.Locale.US, "$%.2f", property.buildingValue) else "N/A"
            val totalAssessedValueString = if (property.assessedValue > 0) String.format(java.util.Locale.US, "$%.2f", property.assessedValue) else "N/A"
            val yearBuiltString = if (property.yearBuilt > 0) "${property.yearBuilt}" else "N/A"
            val squareFootageString = if (property.squareFeet > 0) "${property.squareFeet} sq ft" else "N/A"
            val layoutString = "${property.bedrooms ?: 0} Beds / ${property.bathrooms ?: 0.0} Baths"

            val promptPayload = """
                [SYSTEM INSTRUCTION]
                You are an advanced real estate data compiler and layout engine equipped with Google Search Grounding. Your objective is to synthesize the provided local registry data with real-time web intelligence to create a complete property portfolio.

                CRITICAL INSTRUCTION FOR SENSITIVE METRICS:
                1. Rely EXCLUSIVELY on the provided "=== INTEGRATED LOCAL APP REGISTRIES ===" block for ownership names, deed transactions, tax assessments, and specialized record data. 
                2. Use your live Google Search Grounding capacity to explore the physical address location. Research surrounding neighborhood safety indicators, public block-level crime statistics, regional market conditions, and school ratings.
                3. Integrate these two streams seamlessly into your final output.

                === INTEGRATED LOCAL APP REGISTRIES ===
                Target Physical Address: ${candidate.address}
                ArcGIS Owner Name: ${property.ownerName}
                Parcel Legal ID (PID): ${property.pid}
                Deed Reference: Book ${property.deedBook} / Page ${property.deedPage}
                Last Sale Value: $salePriceString on ${property.lastSaleDate}
                Calculated Annual Tax: $taxAmountString
                Land Value: $landValueString
                Building Value: $buildingValueString
                Total Assessed Value: $totalAssessedValueString
                Year Built: $yearBuiltString
                Square Footage: $squareFootageString
                Interior Layout: $layoutString
                Zoning/Land Use: ${property.zoning}
                App-Sourced Legal/Civil Notes: $footprintString

                === GROUNDING INSTRUCTION ===
                1. Execute a real-time web search focusing on the target physical address and its immediate neighborhood block. 
                2. Gather localized safety insights, neighborhood crime history indices, and proximity risk factors.
                3. Fetch surrounding property market updates and active school zone performance ratings.

                === OUTPUT FORMAT REQUIREMENTS ===
                Synthesize the injected registry metrics and your live search findings into a professional, structured markdown layout. Ensure the true owner name and exact tax histories are cleanly displayed alongside your discovered neighborhood safety metrics and general local property insights.
            """.trimIndent()

            PropertyDataResult(
                formattedData = sb.toString(),
                hasOfficialGISData = hasOfficialGISData,
                coordinates = Pair(x, y),
                promptPayload = promptPayload
            )
        }
    }

    suspend fun fetchPropertyDataString(address: String): String {
        return fetchPropertyData(address).formattedData
    }

    data class SpatialestResolution(val id: Int, val csrfToken: String)

    suspend fun getSpatialestId(address: String, x: Double, y: Double): SpatialestResolution? = withContext(Dispatchers.IO) {
        val urlMain = "https://property.spatialest.com/nc/mecklenburg/"
        val requestMain = Request.Builder()
            .url(urlMain)
            .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        
        try {
            client.newCall(requestMain).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val html = response.body?.string() ?: return@withContext null
                
                // Extract CSRF Token
                var csrfToken = ""
                val regexToken = Regex("\"csrfToken\":\"([^\"]+)\"")
                val match = regexToken.find(html)
                if (match != null) {
                    csrfToken = match.groupValues[1]
                } else {
                    val regexMeta = Regex("name=\"csrf-token\"\\s+content=\"([^\"]+)\"")
                    val matchMeta = regexMeta.find(html)
                    if (matchMeta != null) {
                        csrfToken = matchMeta.groupValues[1]
                    }
                }
                
                if (csrfToken.isEmpty()) {
                    println("Spatialest: CSRF token not found in main page HTML")
                    return@withContext null
                }
                
                // Clean address
                var cleanStreet = address
                val parts = address.split(",")
                if (parts.isNotEmpty()) {
                    cleanStreet = parts[0].trim()
                }
                // Strip zip (anchored to the end of the string to preserve 5-digit house numbers)
                val zipRegex = Regex("\\b\\d{5}(-\\d{4})?\\s*$")
                cleanStreet = zipRegex.replace(cleanStreet, "").trim()
                
                var internalId: Int? = null
                
                // 1. Text search first via POST
                val urlSearchPost = "https://property.spatialest.com/nc/mecklenburg/api/v2/search"
                val bodyObj = JSONObject().apply {
                    put("filters", JSONObject().apply {
                        put("term", cleanStreet)
                    })
                    put("page", 1)
                    put("limit", 21)
                }
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = bodyObj.toString().toRequestBody(mediaType)
                
                val requestPost = Request.Builder()
                    .url(urlSearchPost)
                    .post(body)
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Referer", "https://property.spatialest.com/nc/mecklenburg/")
                    .addHeader("X-CSRF-TOKEN", csrfToken)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .build()
                
                try {
                    client.newCall(requestPost).execute().use { respPost ->
                        if (respPost.isSuccessful) {
                            val resStr = respPost.body?.string()
                            if (resStr != null) {
                                val jsonPost = JSONObject(resStr)
                                if (jsonPost.has("id")) {
                                    internalId = jsonPost.optInt("id")
                                } else if (jsonPost.has("results")) {
                                    val results = jsonPost.optJSONArray("results")
                                    if (results != null) {
                                        for (i in 0 until results.length()) {
                                            val res = results.getJSONObject(i)
                                            val display = res.optJSONArray("display")
                                            if (display != null) {
                                                for (j in 0 until display.length()) {
                                                    val item = display.getJSONObject(j)
                                                    if (item.optString("id") == "location_address") {
                                                        val cleanedVal = item.optString("value").uppercase().trim()
                                                        val cleanedTarget = cleanStreet.uppercase()
                                                        if (cleanedVal == cleanedTarget || cleanedVal.contains(cleanedTarget) || cleanedTarget.contains(cleanedVal)) {
                                                            internalId = res.optInt("ParcelIdentifier")
                                                            break
                                                        }
                                                    }
                                                }
                                            }
                                            if (internalId != null) break
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Spatialest POST search failed: ${e.message}")
                }
                
                // 2. Fallback to coordinate query
                if (internalId == null) {
                    println("Spatialest POST search did not find ID for '$cleanStreet', falling back to coordinates...")
                    val urlSearchString = "https://property.spatialest.com/nc/mecklenburg/api/v2/search?filters[lat]=$y&filters[lng]=$x"
                    val requestSearch = Request.Builder()
                        .url(urlSearchString)
                        .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .addHeader("Referer", "https://property.spatialest.com/nc/mecklenburg/")
                        .addHeader("X-CSRF-TOKEN", csrfToken)
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .build()
                    
                    try {
                        client.newCall(requestSearch).execute().use { respSearch ->
                            if (respSearch.isSuccessful) {
                                val resStr = respSearch.body?.string()
                                if (resStr != null) {
                                    val jsonSearch = JSONObject(resStr)
                                    if (jsonSearch.has("id")) {
                                        internalId = jsonSearch.optInt("id")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Spatialest coordinate search failed: ${e.message}")
                    }
                }
                
                val resolvedId = internalId ?: return@withContext null
                return@withContext SpatialestResolution(resolvedId, csrfToken)
            }
        } catch (e: Exception) {
            println("Spatialest error in getSpatialestId: ${e.message}")
        }
        return@withContext null
    }

    suspend fun resolveSpatialestId(address: String): Int? = withContext(Dispatchers.IO) {
        val addrLower = address.lowercase()
        val normalizedAddress = if (
            addrLower.contains("charlotte") || addrLower.contains(", nc") ||
            addrLower.contains("matthews") || addrLower.contains("mint hill") ||
            addrLower.contains("huntersville") || addrLower.contains("cornelius") ||
            addrLower.contains("davidson") || addrLower.contains("pineville") ||
            addrLower.contains("mooresville") || addrLower.contains("concord")
        ) address else "$address, Charlotte, NC"
        
        var candidate = geocodeAddressCensus(normalizedAddress)
        if (candidate == null) {
            candidate = geocodeAddressGIS(normalizedAddress)
        }
        if (candidate == null) {
            candidate = geocodeAddressNominatim(normalizedAddress)
        }
        
        if (candidate == null) return@withContext null
        return@withContext getSpatialestId(candidate.address, candidate.location.x, candidate.location.y)?.id
    }

    private suspend fun fetchSpatialestData(address: String, x: Double, y: Double): PropertyAttributes? = withContext(Dispatchers.IO) {
        val resolved = getSpatialestId(address, x, y) ?: return@withContext null
        val resolvedId = resolved.id
        val csrfToken = resolved.csrfToken
        
        try {
            println("Spatialest using resolved ID: $resolvedId")
            
            // 3. Query record card
            val urlCardString = "https://property.spatialest.com/nc/mecklenburg/api/v1/recordcard/$resolvedId"
            val requestCard = Request.Builder()
                .url(urlCardString)
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Referer", "https://property.spatialest.com/nc/mecklenburg/")
                    .addHeader("X-CSRF-TOKEN", csrfToken)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .build()
                
                client.newCall(requestCard).execute().use { respCard ->
                    if (!respCard.isSuccessful) return@withContext null
                    val cardStr = respCard.body?.string() ?: return@withContext null
                    val jsonCard = JSONObject(cardStr)
                    val parcel = jsonCard.optJSONObject("parcel") ?: return@withContext null
                    
                    val header = parcel.optJSONObject("header") ?: JSONObject()
                    val sections = parcel.optJSONArray("sections") ?: JSONArray()
                    
                    val prop = PropertyAttributes()
                    prop.pid = header.optString("ParcelID", "")
                    prop.ownerName = header.optString("owners", "None Found")
                    
                    val assessedValueStr = header.optString("PublicTotalMarketValue", "$0")
                    val numericVal = assessedValueStr.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
                    prop.assessedValue = numericVal
                    prop.taxAmount = numericVal * 0.01 // 1% estimation
                    
                    if (sections.length() > 0) {
                        val secDict = sections.optJSONObject(0)
                        if (secDict != null) {
                            val sec1 = secDict.optJSONArray("1")
                            if (sec1 != null && sec1.length() > 0) {
                                val first1 = sec1.getJSONObject(0)
                                val salePriceStr = first1.optString("SalePrice", "$0")
                                prop.lastSalePrice = salePriceStr.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
                                prop.lastSaleDate = first1.optString("SaleDate", "N/A")
                                prop.zoning = first1.optString("landuse_description", "N/A")
                                
                                val legal = first1.optString("LegalDescription", "N/A")
                                if (legal.startsWith("L")) {
                                    val partsLegal = legal.split(" ")
                                    if (partsLegal.size >= 2) {
                                        prop.deedBook = partsLegal[0].drop(1)
                                        if (partsLegal[1].startsWith("M")) {
                                            prop.deedPage = partsLegal[1].drop(1)
                                        }
                                    }
                                }
                            }
                            val sec2 = secDict.optJSONArray("2")
                            if (sec2 != null && sec2.length() > 0) {
                                val first2 = sec2.getJSONObject(0)
                                val landValStr = first2.optString("PublicTotalLandValue", "$0")
                                val bldgValStr = first2.optString("PublicTotalBuildingValue", "$0")
                                prop.landValue = landValStr.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
                                prop.buildingValue = bldgValStr.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
                            }
                        }
                    }
                    
                    if (sections.length() > 2) {
                        val bldgGroup = sections.optJSONArray(2)
                        if (bldgGroup != null && bldgGroup.length() > 0) {
                            val firstGroup = bldgGroup.optJSONArray(0)
                            if (firstGroup != null && firstGroup.length() > 0) {
                                val firstBldg = firstGroup.optJSONObject(0)
                                if (firstBldg != null) {
                                    prop.yearBuilt = firstBldg.optInt("yearbuilt", 0)
                                    if (prop.yearBuilt == 0) {
                                        val ybStr = firstBldg.optString("yearbuilt", "")
                                        prop.yearBuilt = ybStr.toIntOrNull() ?: 0
                                    }
                                    
                                    prop.squareFeet = firstBldg.optInt("finishedarea", 0)
                                    if (prop.squareFeet == 0) {
                                        val sfStr = firstBldg.optString("finishedarea", "").replace(",", "")
                                        prop.squareFeet = sfStr.toIntOrNull() ?: 0
                                    }
                                    
                                    prop.bedrooms = firstBldg.optInt("BedRooms", 0)
                                    if (prop.bedrooms == 0) {
                                        val brStr = firstBldg.optString("BedRooms", "")
                                        prop.bedrooms = brStr.toIntOrNull()
                                    }
                                    
                                    var baths = 0.0
                                    val fb = firstBldg.optDouble("FullBath", 0.0)
                                    baths += fb
                                    
                                    val hb = firstBldg.optDouble("HalfBath", 0.0)
                                    baths += hb * 0.5
                                    
                                    if (baths > 0.0) {
                                        prop.bathrooms = baths
                                    }
                                }
                            }
                        }
                    }
                    
                    return@withContext prop
                }
            } catch (e: Exception) {
                println("Spatialest error: ${e.message}")
            }
            return@withContext null
        }       


    // Private Fetchers & Parsers

    private fun geocodeAddressCensus(street: String): Candidate? {
        try {
            val encodedStreet = URLEncoder.encode(street, "UTF-8")
            val url = "https://geocoding.geo.census.gov/geocoder/locations/onelineaddress?address=$encodedStreet&benchmark=Public_AR_Current&format=json"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                val resultObj = json.optJSONObject("result") ?: return null
                val matches = resultObj.optJSONArray("addressMatches") ?: return null
                if (matches.length() > 0) {
                    val first = matches.getJSONObject(0)
                    val matchedAddress = first.optString("matchedAddress", street)
                    val coords = first.optJSONObject("coordinates") ?: return null
                    val location = Location(coords.optDouble("x"), coords.optDouble("y"))
                    return Candidate(matchedAddress, location, null)
                }
            }
        } catch (e: Exception) {
            println("Census Geocode Error: ${e.message}")
        }
        return null
    }

    private fun geocodeAddressGIS(street: String): Candidate? {
        val encodedStreet = URLEncoder.encode(street, "UTF-8")
        val url = "https://gis.charlottenc.gov/arcgis/rest/services/LOC/MasterAddress/GeocodeServer/findAddressCandidates?SingleLine=$encodedStreet&outSR=$spatialReference&f=json"
        
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
            println("GIS Geocode Error: ${e.message}")
        }
        return null
    }

    private fun geocodeAddressNominatim(street: String): Candidate? {
        val encodedStreet = URLEncoder.encode(street, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?q=$encodedStreet&format=json&limit=1&countrycodes=us"

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "QCAI-App/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val results = JSONArray(jsonStr)
                if (results.length() > 0) {
                    val first = results.getJSONObject(0)
                    val lat = first.optString("lat").toDoubleOrNull() ?: return null
                    val lon = first.optString("lon").toDoubleOrNull() ?: return null
                    val displayName = first.optString("display_name", street)
                    val location = Location(lon, lat)
                    return Candidate(displayName, location, null)
                }
            }
        } catch (e: Exception) {
            println("Nominatim Geocode Error: ${e.message}")
        }
        return null
    }

    private fun fetchParcelInfo(x: Double, y: Double): JSONObject? {
        val url = "https://gis.charlottenc.gov/arcgis/rest/services/CountyData/Parcels/MapServer/0/query?geometry=$x,$y&geometryType=esriGeometryPoint&inSR=$spatialReference&spatialRel=esriSpatialRelIntersects&outFields=*&f=json"
        
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val jsonStr = response.body?.string() ?: return null
                val json = JSONObject(jsonStr)
                val features = json.optJSONArray("features") ?: return null
                if (features.length() > 0) {
                    return features.getJSONObject(0).optJSONObject("attributes")
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

    private fun parsePropertyAttributes(attrs: JSONObject): PropertyAttributes {
        val prop = PropertyAttributes()
        val keys = attrs.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val kl = key.lowercase()
            val valObj = attrs.opt(key)
            if (valObj == null || valObj == JSONObject.NULL) continue
            
            if (kl == "pid" || kl == "pin" || kl == "parcel_id" || kl == "parcelid" || kl == "nc_pin") {
                prop.pid = valObj.toString()
            }
            else if (kl == "owner_name" || kl == "own_name" || kl == "legal_owner" || kl == "owner") {
                prop.ownerName = valObj.toString()
            }
            else if (kl.contains("sale_price") || kl.contains("saleprice") || kl.contains("sale_val") || (kl.contains("price") && !kl.contains("unit"))) {
                prop.lastSalePrice = when (valObj) {
                    is Number -> valObj.toDouble()
                    else -> valObj.toString().toDoubleOrNull() ?: 0.0
                }
            }
            else if (kl.contains("sale_date") || kl.contains("saledate") || kl == "date" || kl.contains("record_date")) {
                if (valObj is Number) {
                    val date = java.util.Date(valObj.toLong())
                    val formatter = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US)
                    prop.lastSaleDate = formatter.format(date)
                } else {
                    prop.lastSaleDate = valObj.toString()
                }
            }
            else if (kl.contains("deed_book") || kl.contains("deedbook") || kl == "book" || kl.contains("book_num")) {
                prop.deedBook = valObj.toString()
            }
            else if (kl.contains("deed_page") || kl.contains("deedpage") || kl == "page" || kl.contains("page_num")) {
                prop.deedPage = valObj.toString()
            }
            else if (kl.contains("assessed_value") || kl.contains("assessed_val") || kl == "total_value" || kl == "total_val" || (kl.contains("value") && !kl.contains("land") && !kl.contains("bldg") && !kl.contains("improvement") && !kl.contains("building"))) {
                prop.assessedValue = when (valObj) {
                    is Number -> valObj.toDouble()
                    else -> valObj.toString().toDoubleOrNull() ?: 0.0
                }
            }
            else if (kl.contains("land_value") || kl.contains("land_val") || kl.contains("landvalue")) {
                prop.landValue = when (valObj) {
                    is Number -> valObj.toDouble()
                    else -> valObj.toString().toDoubleOrNull() ?: 0.0
                }
            }
            else if (kl.contains("building_value") || kl.contains("building_val") || kl.contains("buildingvalue") || kl.contains("bldg_val") || kl.contains("improvement")) {
                prop.buildingValue = when (valObj) {
                    is Number -> valObj.toDouble()
                    else -> valObj.toString().toDoubleOrNull() ?: 0.0
                }
            }
            else if (kl.contains("tax_amount") || kl.contains("tax_amt") || kl.contains("tax_val") || kl == "tax" || kl.contains("tax_levy")) {
                prop.taxAmount = when (valObj) {
                    is Number -> valObj.toDouble()
                    else -> valObj.toString().toDoubleOrNull() ?: 0.0
                }
            }
            else if (kl.contains("year_built") || kl.contains("yearbuilt") || kl.contains("built_year") || kl == "yr_built" || kl == "year") {
                prop.yearBuilt = when (valObj) {
                    is Number -> valObj.toInt()
                    else -> valObj.toString().toIntOrNull() ?: 0
                }
            }
            else if (kl.contains("square_feet") || kl.contains("sq_feet") || kl == "sqft" || kl.contains("heated_area") || kl == "area" || kl.contains("sq_ft")) {
                prop.squareFeet = when (valObj) {
                    is Number -> valObj.toInt()
                    else -> valObj.toString().toIntOrNull() ?: 0
                }
            }
            else if (kl.contains("bedrooms") || kl.contains("bedroom") || kl == "beds" || kl == "bed") {
                prop.bedrooms = when (valObj) {
                    is Number -> valObj.toInt()
                    else -> valObj.toString().toIntOrNull()
                }
            }
            else if (kl.contains("bathrooms") || kl.contains("bathroom") || kl == "baths" || kl == "bath") {
                prop.bathrooms = when (valObj) {
                    is Number -> valObj.toDouble()
                    else -> valObj.toString().toDoubleOrNull()
                }
            }
            else if (kl == "zoning" || kl == "zone" || kl.contains("zoning_class")) {
                prop.zoning = valObj.toString()
            }
        }
        return prop
    }
}
