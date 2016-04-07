package comp207p.target;

public class reassignedIf {
    public boolean foo() {
        int x = 4;
        int y = 6;
        x += y;
        if(x < y) {
            return true;
        }

        return false;
    }
}
