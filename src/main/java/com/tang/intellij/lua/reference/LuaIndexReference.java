package com.tang.intellij.lua.reference;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectAndLibrariesScope;
import com.intellij.util.IncorrectOperationException;
import com.tang.intellij.lua.comment.psi.LuaDocFieldDef;
import com.tang.intellij.lua.lang.type.LuaType;
import com.tang.intellij.lua.lang.type.LuaTypeSet;
import com.tang.intellij.lua.lang.type.LuaTypeTable;
import com.tang.intellij.lua.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 * Created by TangZX on 2016/12/4.
 */
public class LuaIndexReference extends PsiReferenceBase<LuaIndexExpr> {

    private PsiElement id;

    public LuaIndexReference(@NotNull LuaIndexExpr element, PsiElement id) {
        super(element);
        this.id = id;
    }


    @Override
    public TextRange getRangeInElement() {
        int start = id.getTextOffset() - myElement.getTextOffset();
        return new TextRange(start, start + id.getTextLength());
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        PsiElement newId = LuaElementFactory.createIdentifier(myElement.getProject(), newElementName);
        id.replace(newId);
        return newId;
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
        return myElement.getManager().areElementsEquivalent(resolve(), element);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        LuaTypeSet typeSet = myElement.guessPrefixType();
        if (typeSet != null) {
            String idString = id.getText();
            Project project = myElement.getProject();
            GlobalSearchScope scope = new ProjectAndLibrariesScope(project);
            for (LuaType type : typeSet.getTypes()) {
                if (type instanceof LuaTypeTable) { // 可能是 table 字段
                    LuaTypeTable tableType = (LuaTypeTable) type;
                    LuaFieldList fieldList = tableType.tableConstructor.getFieldList();
                    if (fieldList != null) {
                        for (LuaField field : fieldList.getFieldList()) {
                            PsiElement nameId = field.getNameDef();
                            if (nameId != null && idString.equals(nameId.getText())) {
                                return nameId;
                            }
                        }
                    }
                } else {
                    LuaDocFieldDef fieldDef = type.findField(idString, project, scope);
                    if (fieldDef != null)
                        return fieldDef.getFieldNameDef();
                    LuaClassMethodDef methodDef = type.findStaticMethod(idString, true, project, scope);
                    if (methodDef != null)
                        return methodDef.getClassMethodName().getNameDef();
                }
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}
