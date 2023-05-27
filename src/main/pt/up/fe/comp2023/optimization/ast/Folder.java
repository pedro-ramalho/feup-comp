package pt.up.fe.comp2023.optimization.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;

public class Folder {
    public Folder() {}

    public void fold(JmmNode updated, JmmNode old) {
        JmmNode parent = old.getJmmParent();

        int position = parent.getChildren().indexOf(old);

        parent.removeJmmChild(position);
        parent.add(updated, position);

        updated.setParent(parent);

    }
}
