package testLib;

public class ConstructorMovedToSuperclass extends ConstructorMovedToSuperclass_Superclass {
    public ConstructorMovedToSuperclass() {
        super(0);
    }
}

class ConstructorMovedToSuperclass_Superclass {
    public ConstructorMovedToSuperclass_Superclass(int i) {
    }
}
