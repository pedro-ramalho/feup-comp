package pt.up.fe.comp2023.optimization.ast.utils;

import pt.up.fe.comp.jmm.ast.JmmNode;

public class Replacer {

    public Replacer() {}

    public void exec(JmmNode updated, JmmNode original) {
        JmmNode parent = original.getJmmParent();

        /* no parent, ignore the replacement */
        if (parent == null)
            return;

        int position = parent.getChildren().indexOf(original);

        parent.removeJmmChild(original);
        parent.add(updated, position);

        updated.setParent(parent);
    }
}


