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

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tri.util.json.jsonMapper
import java.time.LocalDate

class WeatherServiceTest {

    val testJson = """
        {
          "coord": {
            "lon": -76.7983,
            "lat": 39.2673
          },
          "weather": [
            {
              "id": 803,
              "main": "Clouds",
              "description": "broken clouds",
              "icon": "04d"
            }
          ],
          "base": "stations",
          "main": {
            "temp": 52.11,
            "feels_like": 49.55,
            "temp_min": 48,
            "temp_max": 56.55,
            "pressure": 1005,
            "humidity": 54
          },
          "visibility": 10000,
          "wind": {
            "speed": 24.16,
            "deg": 280,
            "gust": 40.29
          },
          "clouds": {
            "all": 75
          },
          "dt": 1677952457,
          "sys": {
            "type": 2,
            "id": 2012057,
            "country": "US",
            "sunrise": 1677929765,
            "sunset": 1677970930
          },
          "timezone": -18000,
          "id": 4354265,
          "name": "Ellicott City",
          "cod": 200
        }
    """.trimIndent()

    @Test
    fun testParse() {
        val response = jsonMapper.readValue<WeatherResponse>(testJson)
        Assertions.assertEquals(52.11, response.main.temp)
        Assertions.assertEquals(75, response.clouds!!.all)
        Assertions.assertEquals(24.16, response.wind!!.speed)
    }

    @Test
    fun testWeatherService() {
        val city = "New York" // Replace with your desired city

        val result = weatherService.getWeather(WeatherRequest(city))
        if (result == null)
            println("Invalid request or no API key found.")
        else {
            println("Today's forecast for $city: ${result.description}, temperature: ${result.temperature}°F")

            val yesterday = LocalDate.now().minusDays(1)
            val result2 = weatherService.getWeather(WeatherRequest(city, yesterday))!!
            println("Yesterday's forecast for $city: ${result2.description}, temperature: ${result2.temperature}°F")
        }
    }

}
