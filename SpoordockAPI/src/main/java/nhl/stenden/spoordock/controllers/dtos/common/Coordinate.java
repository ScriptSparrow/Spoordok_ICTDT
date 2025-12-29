package nhl.stenden.spoordock.controllers.dtos.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Coordinate {

    private double x;
    private double y;
    private double z;

    public Coordinate() {}

    public Coordinate(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
