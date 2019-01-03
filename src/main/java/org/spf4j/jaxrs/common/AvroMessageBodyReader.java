package org.spf4j.jaxrs.common;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.specific.SpecificDatumReader;
import org.spf4j.http.Headers;
import org.spf4j.io.MemorizingBufferedInputStream;

/**
 *
 * @author Zoltan Farkas
 */
public abstract class AvroMessageBodyReader implements MessageBodyReader<Object> {

  private final SchemaResolver client;

  @Inject
  public AvroMessageBodyReader(final SchemaResolver client) {
    this.client = client;
  }

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return true;
  }

  public abstract Decoder getDecoder(final Schema writerSchema, final InputStream is)
          throws IOException;

  public  InputStream wrapInputStream(InputStream pentityStream) {
    return new MemorizingBufferedInputStream(pentityStream);
  }

  @Override
  public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations,
          MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream pentityStream)
          throws IOException, WebApplicationException {
    String schemaStr = httpHeaders.getFirst(Headers.CONTENT_SCHEMA);
    Schema writerSchema = new Schema.Parser(new AvroNamesRefResolver(client)).parse(schemaStr);
    Schema readerSchema = ReflectData.get().getSchema(genericType);
    DatumReader reader = new SpecificDatumReader(writerSchema, readerSchema);
    InputStream entityStream = wrapInputStream(pentityStream);
    try {
      Decoder decoder = getDecoder(writerSchema, entityStream);
      return reader.read(null, decoder);
    } catch (IOException | RuntimeException ex) {
      throw new RuntimeException(this + " parsing failed for " + writerSchema + ", from " + entityStream, ex);
    }
  }

}
