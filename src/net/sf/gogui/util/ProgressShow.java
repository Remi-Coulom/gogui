// ProgressShow.java

package net.sf.gogui.util;

/** Callback for indicating progress made by long running functions. */
public interface ProgressShow
{
    void showProgress(int percent);
}
