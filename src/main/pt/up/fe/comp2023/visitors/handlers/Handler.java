package pt.up.fe.comp2023.visitors.handlers;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.MySymbolTable;
import pt.up.fe.comp2023.visitors.utils.MyType;

public interface Handler {
    public Type getType();
}
