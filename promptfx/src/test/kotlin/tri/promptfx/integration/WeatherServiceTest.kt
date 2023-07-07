package tri.promptfx.integration

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tri.ai.openai.mapper

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
        val response = mapper.readValue<WeatherResponse>(testJson)
        Assertions.assertEquals(52.11, response.main.temp)
        Assertions.assertEquals(75, response.clouds!!.all)
        Assertions.assertEquals(24.16, response.wind!!.speed)
    }

}