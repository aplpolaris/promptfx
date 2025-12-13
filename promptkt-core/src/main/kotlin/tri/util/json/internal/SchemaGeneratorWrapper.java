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
package tri.util.json.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;

/**
 * Wrapper class to handle jsonschema-generator library from Java.
 * This avoids Kotlin compiler issues with modular JARs.
 */
public class SchemaGeneratorWrapper {
    private static final SchemaGenerator SCHEMA_GENERATOR;
    
    static {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
            SchemaVersion.DRAFT_2020_12, 
            OptionPreset.PLAIN_JSON
        );
        configBuilder.with(new JacksonModule());
        SCHEMA_GENERATOR = new SchemaGenerator(configBuilder.build());
    }
    
    /**
     * Generates a JSON schema from a Java class.
     * @param clazz the class to generate schema for
     * @return the JSON schema as a JsonNode
     */
    public static JsonNode generateSchema(Class<?> clazz) {
        return SCHEMA_GENERATOR.generateSchema(clazz);
    }
}
