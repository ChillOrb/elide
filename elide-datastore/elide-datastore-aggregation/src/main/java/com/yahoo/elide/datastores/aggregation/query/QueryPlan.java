/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection.innerQueryProjections;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection.outerQueryProjections;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.google.common.collect.Streams;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link QueryPlan} is a partial Query bound to a particular Metric.
 */
@Value
@Builder
public class QueryPlan implements Queryable {

    @NonNull
    private Queryable source;

    @Singular
    @NonNull
    private Set<MetricProjection> metricProjections;

    @Singular
    @NonNull
    private Set<ColumnProjection> dimensionProjections;

    @Singular
    @NonNull
    private Set<TimeDimensionProjection> timeDimensionProjections;

    /**
     * Merges two query plans together.  The order of merged metrics and dimensions is preserved such that
     * the current plan metrics and dimensions come after the requested plan metrics and dimensions.
     * @param other The other query to merge.
     * @return A new merged query plan.
     */
    public QueryPlan merge(QueryPlan other, SQLReferenceTable lookupTable) {
        QueryPlan self = this;

        if (other == null) {
            return this;
        }

        while (other.nestDepth() > self.nestDepth()) {
            self = self.nest(lookupTable);
        }

        while (self.nestDepth() > other.nestDepth()) {
            other = other.nest(lookupTable);
        }

        assert (self.isNested() || getSource().equals(other.getSource()));

        Set<MetricProjection> metrics = Streams.concat(other.metricProjections.stream(),
                self.metricProjections.stream()).collect(Collectors.toCollection(LinkedHashSet::new));

        Set<TimeDimensionProjection> timeDimensions = Streams.concat(other.timeDimensionProjections.stream(),
                self.timeDimensionProjections.stream()).collect(Collectors.toCollection(LinkedHashSet::new));

        Set<ColumnProjection> dimensions = Streams.concat(other.dimensionProjections.stream(),
                self.dimensionProjections.stream()).collect(Collectors.toCollection(LinkedHashSet::new));

        if (!self.isNested()) {
            return QueryPlan.builder()
                    .source(self.getSource())
                    .metricProjections(metrics)
                    .dimensionProjections(dimensions)
                    .timeDimensionProjections(timeDimensions)
                    .build();
        } else {
            Queryable mergedSource = ((QueryPlan) self.getSource()).merge((QueryPlan) other.getSource(), lookupTable);
            return QueryPlan.builder()
                    .source(mergedSource)
                    .metricProjections(metrics)
                    .dimensionProjections(dimensions)
                    .timeDimensionProjections(timeDimensions)
                    .build();
        }
    }

    /**
     * Breaks a flat query into a nested query.  There are multiple approaches for how to do this, but
     * this kind of nesting requires aggregation to happen both in the inner and outer queries.  This allows
     * query plans with two-pass aggregations to be merged with simpler one-pass aggregation plans.
     *
     * The nesting performed here attempts to perform all joins in the inner query.
     *
     * @param lookupTable Needed for answering questions about templated SQL column definitions.
     * @return A nested query plan.
     */
    public QueryPlan nest(SQLReferenceTable lookupTable) {
        QueryPlan inner = QueryPlan.builder()
                .source(this.getSource())
                .metricProjections(innerQueryProjections(source, metricProjections, lookupTable, false))
                .dimensionProjections(innerQueryProjections(source, dimensionProjections, lookupTable, false))
                .timeDimensionProjections(innerQueryProjections(source, timeDimensionProjections,
                        lookupTable, false))
                .build();

        return QueryPlan.builder()
                .source(inner)
                .metricProjections(outerQueryProjections(source, metricProjections, lookupTable, false))
                .dimensionProjections(outerQueryProjections(source, dimensionProjections, lookupTable, false))
                .timeDimensionProjections(outerQueryProjections(source, timeDimensionProjections,
                        lookupTable, false))
                .build();
    }
}
