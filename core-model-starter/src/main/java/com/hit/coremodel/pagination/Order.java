package com.hit.coremodel.pagination;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Order {

    @Parameter(description = "The name of property want to sort.")
    private String name;

    @Parameter(description = "Sorting criteria: ASC|DESC. Default sort order is descending.")
    private String direction = Direction.DESC.name();

    public String getDirection() {
        return this.direction.trim().toUpperCase();
    }

    public boolean isDescending() {
        return Direction.DESC.name().equals(this.getDirection());
    }

    public boolean isAscending() {
        return Direction.ASC.name().equals(this.getDirection());
    }

    public enum Direction {
        ASC, DESC;
    }
}
