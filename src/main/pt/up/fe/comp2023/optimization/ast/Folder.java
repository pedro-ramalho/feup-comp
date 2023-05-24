package pt.up.fe.comp2023.optimization.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;

public class Folder {
    public Folder() {}

    public void replace(JmmNode newNode, JmmNode oldNode) {
        JmmNode parent = oldNode.getJmmParent();

        int position = parent.getChildren().indexOf(oldNode);

        parent.removeJmmChild(position);
        parent.add(newNode, position);

        newNode.setParent(parent);
    }
}
