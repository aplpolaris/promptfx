package tri.promptfx.integration

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.openai.OpenAiSettings
import tri.ai.openai.mapper
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

private val today = LocalDate.now()

data class WeatherRequest(val city: String, val date: LocalDate = today, val historical: Boolean = date != today)
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
    val baseUrl = "http://api.openweathermap.org/data/2.5"
    override fun getWeather(request: WeatherRequest): WeatherResult? {
        try {
            val endpoint = if (request.historical) "history" else "weather"
            val url = "$baseUrl/$endpoint?q=${request.city}&appid=$apiKey&units=imperial"
            println(url)
            val response = URL(url).readText()
            val responseObj = mapper.readValue<WeatherResponse>(response)

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
    fun Int.epochSecondsToLocalDateTime(): LocalDateTime =
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

fun main() {
    val city = "New York" // Replace with your desired city

    val result = weatherService.getWeather(WeatherRequest(city))
    if (result == null)
        println("Invalid request or no API key found.")
    else {
        println("Today's forecast for $city: ${result.description}, temperature: ${result.temperature}°F")

        val yesterday = today.minusDays(1)
        val result2 = weatherService.getWeather(WeatherRequest(city, yesterday))!!
        println("Yesterday's forecast for $city: ${result2.description}, temperature: ${result2.temperature}°F")
    }
}