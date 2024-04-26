package org.constellation.process.dynamic.galaxy.model.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.constellation.process.dynamic.galaxy.model.Status;

import java.io.IOException;

public class CaseInsensitiveStatusDeserializer extends StdDeserializer<Status> {
    public CaseInsensitiveStatusDeserializer() {
        super(Status.class);
    }

    @Override
    public Status deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getText().toLowerCase();
        return Status.fromValue(value);
    }
}