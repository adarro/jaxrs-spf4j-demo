
package org.spf4j.demo;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.Path;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.avro.Projections;
import org.spf4j.demo.avro.DemoRecord;
import org.spf4j.demo.avro.DemoRecordInfo;
import org.spf4j.demo.avro.MetaData;
import org.spf4j.log.ExecContextLogger;

/**
 *
 * @author Zoltan Farkas
 */
@Path("example/records")
public class ExampleResourceImpl implements ExampleResource {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(ExampleResourceImpl.class));

 public List<DemoRecordInfo>  getRecords() {
   return Arrays.asList(
       DemoRecordInfo.newBuilder()
           .setDemoRecord(DemoRecord.newBuilder().setId("1")
           .setName("test").setDescription("testDescr").build())
           .setMetaData(MetaData.newBuilder()
                   .setAsOf(Instant.now()).setLastAccessed(Instant.now())
                   .setLastModified(Instant.now())
                   .setLastAccessedBy("you").setLastModifiedBy("you").build()
           ).build());
 }

 public void saveRecords(List<DemoRecordInfo> records) {
   LOG.debug("Received", records);
 }

  @Override
  public <T> List<GenericRecord> getRecordsProjection(Schema elementProjection) {
    return (List<GenericRecord>) Projections.project(Schema.createArray(elementProjection),
            Schema.createArray(DemoRecordInfo.getClassSchema()), getRecords());
  }


}