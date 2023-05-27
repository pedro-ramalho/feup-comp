package pt.up.fe.comp2023.optimization.ast.utils;

import pt.up.fe.comp.jmm.ast.JmmNode;

public class Replacer {
    private JmmNode updated;
    private JmmNode original;

    public Replacer(JmmNode updated, JmmNode original) {
        this.updated = updated;
        this.original = original;
    }

    public void exec() {
        JmmNode parent = this.original.getJmmParent();

        /* no parent, ignore the replacement */
        if (parent == null)
            return;

        int position = parent.getChildren().indexOf(original);

        parent.removeJmmChild(this.original);
        parent.add(this.updated, position);

        this.updated.setParent(parent);
    }
}


