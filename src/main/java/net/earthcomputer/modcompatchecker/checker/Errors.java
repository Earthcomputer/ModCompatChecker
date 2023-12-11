package net.earthcomputer.modcompatchecker.checker;

public enum Errors {
    CLASS_EXTENDS_REMOVED("Superclass %s is removed"),
    CLASS_EXTENDS_FINAL("Superclass %s is final"),
    CLASS_EXTENDS_INTERFACE("Superclass %s is an interface"),
    CLASS_EXTENDS_SEALED("Superclass %s is sealed, and this class is not in its 'permits' clause"),
    CLASS_EXTENDS_INACCESSIBLE("Superclass %s is inaccessible with %s visibility"),
    CLASS_IMPLEMENTS_REMOVED("Superinterface %s is removed"),
    CLASS_IMPLEMENTS_CLASS("Superinterface %s is a class"),
    CLASS_IMPLEMENTS_SEALED("Superinterface %s is sealed, and this class is not in its 'permits' clause"),
    CLASS_IMPLEMENTS_INACCESSIBLE("Superinterface %s is inaccessible with %s visibility"),
    FIELD_TYPE_REMOVED("Field type %s is removed"),
    FIELD_TYPE_INACCESSIBLE("Field type %s is inaccessible with %s visibility"),
    METHOD_RETURN_TYPE_REMOVED("Method return type %s is removed"),
    METHOD_RETURN_TYPE_INACCESSIBLE("Method return type %s is inaccessible with %s visibility"),
    METHOD_PARAM_TYPE_REMOVED("Method parameter type %s is removed"),
    METHOD_PARAM_TYPE_INACCESSIBLE("Method parameter type %s is inaccessible with %s visibility"),
    METHOD_THROWS_TYPE_REMOVED("Method throws type %s is removed"),
    METHOD_THROWS_TYPE_INACCESSIBLE("Method throws type %s is inaccessible with %s visibility"),
    METHOD_OVERRIDES_FINAL("Method overrides final method"),
    ABSTRACT_METHOD_UNIMPLEMENTED("Class does not implement the abstract method %s.%s %s"),
    INCORRECT_INTERFACE_METHOD_LOOKUP("Class does not override the interface method %s.%s %s, resulting in it resolving to the %s method in %s"),
    ;

    private final String description;

    Errors(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
