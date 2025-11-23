/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.core

/**
 * Exception thrown when an external service call fails.
 * This is a domain-specific wrapper for infrastructure exceptions like IOException, HTTP errors, etc.
 */
open class ExternalServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Exception thrown when a weather service call fails.
 */
class WeatherServiceException(message: String, cause: Throwable? = null) : ExternalServiceException(message, cause)

/**
 * Exception thrown when a Wikipedia API call fails.
 */
class WikipediaServiceException(message: String, cause: Throwable? = null) : ExternalServiceException(message, cause)

/**
 * Exception thrown when a web crawling operation fails.
 */
class WebCrawlerException(message: String, cause: Throwable? = null) : ExternalServiceException(message, cause)
