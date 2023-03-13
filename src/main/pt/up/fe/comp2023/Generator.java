package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;

public class Generator extends AJmmVisitor<String, String> {
    private String className;

    public Generator(String className) {
        this.className = className;
    }

    @Override
    protected void buildVisitor() {
        // addVisit(...)
    }
}
