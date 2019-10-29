package org.spf4j.demo.resources.aql;

import java.util.Arrays;
import javax.ws.rs.Path;
import org.spf4j.demo.aql.DataSetResource;
import org.spf4j.demo.aql.Planet;

/**
 *
 * @author Zoltan Farkas
 */
@Path("avql/planets")
public class PlanetsResourceImpl implements DataSetResource<Planet> {

  @Override
  public Iterable<Planet> getData(final String where, final String select) {
    return Arrays.asList(new Planet("earth", "M", 512731872312L),
            new Planet("vulcan", "M", 612731872312L),
            new Planet("andoria", "M", 602731872312L));
  }
;

}
