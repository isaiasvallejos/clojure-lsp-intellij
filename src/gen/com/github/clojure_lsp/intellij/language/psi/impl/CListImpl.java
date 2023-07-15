// This is a generated file. Not intended for manual editing.
package com.github.clojure_lsp.intellij.language.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.github.clojure_lsp.intellij.language.psi.ClojureTypes.*;
import com.github.clojure_lsp.intellij.language.psi.*;

public class CListImpl extends CLVFormImpl implements CList {

  public CListImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull ClojureVisitor visitor) {
    visitor.visitList(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ClojureVisitor) accept((ClojureVisitor)visitor);
    else super.accept(visitor);
  }

}
