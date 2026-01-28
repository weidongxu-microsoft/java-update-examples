package com.microsoft.azure.documentdb.sample.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TodoItem {
    private String category;
    private boolean complete;
    private String id;
    private String name;
}
