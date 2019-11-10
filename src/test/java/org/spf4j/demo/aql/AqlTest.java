
package org.spf4j.demo.aql;

import java.util.Map;
import javax.ws.rs.client.Entity;
import org.spf4j.demo.*;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.CloseableIterable;
import org.spf4j.log.Level;
import org.spf4j.test.log.annotations.PrintLogs;

/**
 *
 * @author Zoltan Farkas
 */
public class AqlTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(AqlTest.class);


  @Test
  public void testGetSchemas() {
    Map<String, Schema> schemas =
            getTarget().path("avql/query/schemas")
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<Map<String, Schema>>() {});
    LOG.debug("Received", schemas);
  }


  @Test
  public void testGetSchema() {
    Schema schema =
            getTarget().path("avql/query/schemas/{sname}").resolveTemplate("sname", "planets")
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<Schema>() {});
    LOG.debug("Received", schema);
  }

  @Test
  public void testGetPlanets() {
    try (CloseableIterable<Planet> planets =
            getTarget().path("avql/planets")
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<CloseableIterable<Planet>>() {})) {
      int i = 0;
      for (Planet planet : planets) {
        LOG.debug("Received", planet);
        i++;
      }
      Assert.assertEquals(3, i);
    }
  }

  @Test
  public void testGetPlanetsFiltered() {
    try (CloseableIterable<GenericRecord> planets =
            getTarget().path("avql/planets")
                    .queryParam("_where", "name = 'earth'")
                    .queryParam("_project", "name,planetClass")
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<CloseableIterable<GenericRecord>>() {})) {
      int i = 0;
      for (GenericRecord planet : planets) {
        LOG.debug("Received", planet);
        i++;
      }
      Assert.assertEquals(1, i);
    }
  }


  @Test
  public void testGetSpecies() {
    try (CloseableIterable<Species> species =
            getTarget().path("avql/species")
                    .queryParam("_where", "name = 'human'")
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<CloseableIterable<Species>>() {})) {
      int i = 0;
      for (Species s : species) {
        LOG.debug("Received", s);
        i++;
      }
      Assert.assertEquals(1, i);
    }
  }

  @Test
  public void testGetCharacters() {
    try (CloseableIterable<GenericRecord> characters =
            getTarget().path("avql/characters")
                    .queryParam("_where", "name like 'J%'")
                    .queryParam("_project", "name,speciesName")
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<CloseableIterable<GenericRecord>>() {})) {
      int i = 0;
      for (GenericRecord s : characters) {
        LOG.debug("Received", s);
        Assert.assertTrue(((String) s.get("name")).startsWith("J"));
        i++;
      }
      Assert.assertEquals(1, i);
    }
  }

  @Test
  public void testGetQuerySimple() {
    try (CloseableIterable<GenericRecord> character =
            getTarget().path("avql/query")
                    .queryParam("query", "select * from characters c")
                    .request(MediaType.valueOf("application/avro"))
                    .get(new GenericType<CloseableIterable<GenericRecord>>() {})) {
      int i = 0;
      for (GenericRecord c : character) {
        LOG.debug("Received", c);
        i++;
      }
      Assert.assertEquals(5, i);
    }
  }

  @Test
  public void testGetQueryProjectFilter() {
    try (CloseableIterable<GenericRecord> character =
            getTarget().path("avql/query")
                    .queryParam("query", "select name from characters where speciesName='vulcan'")
                    .request(MediaType.valueOf("application/avro"))
                    .get(new GenericType<CloseableIterable<GenericRecord>>() {})) {
      int i = 0;
      for (GenericRecord c : character) {
        LOG.debug("Received", c);
        Assert.assertEquals("Spock", c.get("name"));
        i++;
      }
      Assert.assertEquals(1, i);
    }
  }

  @Test
  @PrintLogs(category = "org.codehaus.janino", ideMinLevel = Level.INFO, greedy = true)
  public void testGetQueryJoin() {
    try (CloseableIterable<GenericRecord> characters =
            getTarget().path("avql/query")
                    .queryParam("query", "select c.name "
                            + " from characters c, species s"
                            + " where c.speciesName = s.name and s.originPlanet = 'earth'")
                    .request(MediaType.valueOf("application/avro"))
                    .get(new GenericType<CloseableIterable<GenericRecord>>() {})) {
      int i = 0;
      for (GenericRecord character : characters) {
        LOG.debug("Received", character);
        i++;
      }
      Assert.assertEquals(3, i);
    }
  }

  @Test
  @PrintLogs(category = "org.codehaus.janino", ideMinLevel = Level.INFO, greedy = true)
  public void testGetQueryPlan() {
    CharSequence plan =
            getTarget().path("avql/query/plan")
                    .request(MediaType.valueOf("text/plain"))
                    .post(Entity.text("select name,"
                            + " ARRAY(select c2.name from friendships f, characters c2"
                            + " where f.characterId1 = c.characterId and f.characterId2 = c2.characterId) as friends"
                            + " from characters c"), CharSequence.class);
    LOG.debug("Plan received", plan);
    Assert.assertThat(plan.toString(), Matchers.containsString("LogicalProject(characterId=[$0], name=[$1])"));
  }


  @Test
  @PrintLogs(category = "org.codehaus.janino", ideMinLevel = Level.INFO, greedy = true)
  @Ignore // Calcite interpreter does not seem to implement Corelations.
  public void testGetQuery() {
    try (CloseableIterable<GenericRecord> characters =
            getTarget().path("avql/query")
                    .queryParam("query", "select name,"
                            + " ARRAY(select c2.name from friendships f, characters c2"
                            + " where f.characterId1 = c.characterId and f.characterId2 = c2.characterId) as friends"
                            + " from characters c")
                    .request(MediaType.valueOf("application/avro"))
                    .get(new GenericType<CloseableIterable<GenericRecord>>() {})) {
      int i = 0;
      for (GenericRecord charracter : characters) {
        LOG.debug("Received", charracter);
        i++;
      }
      Assert.assertEquals(4, i);
    }
  }



}