package pt.up.fe.comp2023.optimization.ast.utils;

public class Variable {
    private final String identifier;
    private final String value;

    public Variable(String identifier, String value) {
        this.identifier = identifier;
        this.value = value;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public String getValue() {
        return this.value;
    }
}
