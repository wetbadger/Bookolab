package com.bookolab.restservice.config;

import org.springframework.context.annotation.Configuration;

import tools.jackson.core.StreamWriteConstraints;

/*
Pages are serialized as a nested nextWord chain, one JSON level per word. Jackson's default
write limit is 500 nesting levels, and exceeding it aborts the response mid-write (a truncated
200 instead of an error). Pages briefly hold more than 500 words whenever a burst of words
lands between two repagination runs, so allow headroom. Kept well below the point where
Jackson's recursive serializer itself overflows the JVM thread stack (~1500+ levels).

Jackson 3 (Spring Boot 4) has no configuration property for this; overriding the global default
in a static initializer guarantees it is applied before the auto-configured mapper is built.
*/
@Configuration
public class JacksonConfig {

    static {
        StreamWriteConstraints.overrideDefaultStreamWriteConstraints(
                StreamWriteConstraints.builder()
                        .maxNestingDepth(1000)
                        .build());
    }
}
