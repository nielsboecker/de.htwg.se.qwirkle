package util.state;

/**
 * Created by opuee on 20.01.16.
 */
public class Star extends Shape {
    @Override
    public String getANSI() {
        return "ST";
    }

    @Override
    public String toString() {
        return "STAR";
    }
}
