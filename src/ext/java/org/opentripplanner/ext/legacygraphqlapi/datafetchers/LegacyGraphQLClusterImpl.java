package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.stopclustering.models.StopCluster;
import org.opentripplanner.model.*;

import java.util.function.Function;

public class LegacyGraphQLClusterImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLCluster {
  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> getValue(
            environment,
            cluster -> new Relay.ResolvedGlobalId("Cluster", cluster.getId().toString())
    );
  }

  @Override
  public DataFetcher<String> gtfsId() {
    return environment -> getValue(
            environment,
            cluster -> cluster.getId().toString()
    );
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> getValue(environment, StopCluster::getName);
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getValue(environment, StopCluster::getLat);
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getValue(environment, StopCluster::getLon);
  }

  @Override
  public DataFetcher<Iterable<Stop>> stops() {
    return environment -> getValue(environment, StopCluster::getChildStops);
  }


  private <T> T getValue(
          DataFetchingEnvironment environment,
          Function<StopCluster, T> clusterTFunction
  ) {
    Object source = environment.getSource();
    if (source instanceof StopCluster) {
      return clusterTFunction.apply((StopCluster) source);
    }
    return null;
  }
}
