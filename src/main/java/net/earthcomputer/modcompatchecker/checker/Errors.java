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
    DIAMOND_PROBLEM("Class inherits multiple non-abstract implementations of method %s %s from different interfaces"),
    CODE_REFERENCES_REMOVED_CLASS("Code accesses class %s which is removed"),
    CODE_REFERENCES_INACCESSIBLE_CLASS("Code accesses class %s which is inaccessible with %s visibility"),
    ACCESS_REMOVED_FIELD("Code accesses field %s.%s : %s which is removed"),
    ACCESS_INACCESSIBLE_FIELD("Code accesses field %s.%s : %s which is inaccessible with %s visibility"),
    NONSTATIC_ACCESS_TO_STATIC_FIELD("Code accesses static field %s.%s : %s in a non-static way"),
    STATIC_ACCESS_TO_NONSTATIC_FIELD("Code accesses non-static field %s.%s : %s in a static way"),
    WRITE_FINAL_FIELD("Code writes to final field %s.%s : %s"),
    ACCESS_REMOVED_METHOD("Code accesses method %s.%s %s which is removed"),
    ACCESS_INACCESSIBLE_METHOD("Code accesses method %s.%s %s which is inaccessible with %s visibility"),
    INTERFACE_CALL_TO_NON_INTERFACE_METHOD("Code accesses non-interface method %s.%s %s as if it were in an interface"),
    NON_INTERFACE_CALL_TO_INTERFACE_METHOD("Code accesses interface method %s.%s %s as if it were not in an interface"),
    INTERFACE_CALL_TO_PACKAGE_OR_PROTECTED("Code accesses interface method %s.%s %s with %s visibility"),
    NONSTATIC_CALL_TO_STATIC_METHOD("Code accesses static method %s.%s %s in a non-static way"),
    STATIC_CALL_TO_NONSTATIC_METHOD("Code accesses non-static method %s.%s %s in a static way"),
    INVOKESPECIAL_ABSTRACT_METHOD("Code invokes abstract method %s.%s %s"),
    INVOKESPECIAL_DIAMOND_PROBLEM("Code invokes method %s.%s %s, which has multiple implementations from different interfaces"),
    INSTANTIATING_ABSTRACT_CLASS("Code instantiates abstract class %s"),
    INSTANTIATING_INTERFACE("Code instantiates interface %s"),
    ;

    private final String description;

    Errors(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
