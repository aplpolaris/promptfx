/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.promptfx.`fun`

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.readValue
import tri.util.json.jsonMapper
import java.io.File
import java.io.IOException
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.logging.Logger

interface WeatherService {
    fun getWeather(request: WeatherRequest): WeatherResult?
}

data class WeatherRequest(val city: String, val date: LocalDate = LocalDate.now(), val historical: Boolean = date != LocalDate.now())

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeatherResult(
    val request: WeatherRequest,
    val category: String,
    val description: String,
    val temperature: Double,
    val rain3Hour: Double? = null,
    val windSpeed: Double? = null,
    val cloudCover: Int? = null,
    val sunrise: LocalDateTime? = null,
    val sunset: LocalDateTime? = null,
)

class OpenWeatherMapService(private val apiKey: String) : WeatherService {
    private val baseUrl = "http://api.openweathermap.org/data/2.5"
    override fun getWeather(request: WeatherRequest): WeatherResult? {
        try {
            val endpoint = if (request.historical) "history" else "weather"
            val url = "$baseUrl/$endpoint?q=${request.city}&appid=$apiKey&units=imperial"
            println(url)
            val response = URL(url).readText()
            val responseObj = jsonMapper.readValue<WeatherResponse>(response)

            return WeatherResult(request,
                category = responseObj.weather[0].main,
                description = responseObj.weather[0].description,
                temperature = responseObj.main.temp,
                rain3Hour = responseObj.rain?.`3h`,
                windSpeed = responseObj.wind?.speed,
                cloudCover = responseObj.clouds?.all,
                sunrise = responseObj.sys?.sunrise?.epochSecondsToLocalDateTime(),
                sunset = responseObj.sys?.sunset?.epochSecondsToLocalDateTime()
            )
        } catch (e: IOException) {
            Logger.getLogger(WeatherService::class.java.name).warning(
                "There was an error retrieving data. This might be caused by an invalid city name or a missing API key. " +
                        "The API key should be saved in a file named \"apikey-weather.txt\" in the root directory. " +
                        "To get an API key, please visit https://openweathermap.org/appid."
            )
        }
        return null
    }
    private fun Int.epochSecondsToLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochSecond(this.toLong()), java.time.ZoneId.systemDefault())
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeatherResponse(
    val main: WeatherResponseMain,
    val weather: List<WeatherResponseWeather>,
    val rain: WeatherResponseRain? = null,
    val wind: WeatherResponseWind? = null,
    val clouds: WeatherResponseClouds? = null,
    val sys: WeatherResponseSys? = null,
)

data class WeatherResponseMain(val temp: Double)
data class WeatherResponseWeather(val main: String, val description: String)
data class WeatherResponseRain(val `3h`: Double? = null, val `1h`: Double? = null)
data class WeatherResponseClouds(val all: Int? = null)
data class WeatherResponseWind(val speed: Double? = null)
data class WeatherResponseSys(val sunrise: Int? = null, val sunset: Int? = null)

private val apiKey = File("apikey-weather.txt").let { if (it.exists()) it.readText() else "" }
val weatherService = OpenWeatherMapService(apiKey)
