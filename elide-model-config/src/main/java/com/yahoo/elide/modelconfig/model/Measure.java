/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Measures represent metrics that can be aggregated at query time.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "friendlyName",
    "description",
    "category",
    "hidden",
    "readAccess",
    "definition",
    "queryPlanResolver",
    "type",
    "tags",
    "arguments",
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Measure implements Named {

    @JsonProperty("name")
    private String name;

    @JsonProperty("friendlyName")
    private String friendlyName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("category")
    private String category;

    @JsonProperty("hidden")
    @Builder.Default
    private Boolean hidden = false;

    @JsonProperty("readAccess")
    @Builder.Default
    private String readAccess = "Prefab.Role.All";

    @JsonProperty("definition")
    private String definition;

    @JsonProperty("type")
    private Type type;

    @JsonProperty("queryPlanResolver")
    private String queryPlanResolver;

    @JsonProperty("tags")
    @JsonDeserialize(as = LinkedHashSet.class)
    @Builder.Default
    private Set<String> tags = new LinkedHashSet<>();

    @JsonProperty("arguments")
    @Builder.Default
    private List<Argument> arguments = new ArrayList<>();

    /**
     * Returns description of the measure.
     * If null, returns the name.
     * @return description
     */
    public String getDescription() {
        return (this.description == null ? getName() : this.description);
    }

    /**
     * Checks if this measure has provided argument.
     * @param argName
     * @return true if this measure has provided argument.
     */
    public boolean hasArgument(String argName) {
        return hasName(this.arguments, argName);
    }
}
