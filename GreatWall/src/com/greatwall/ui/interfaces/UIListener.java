package com.greatwall.ui.interfaces;

public interface UIListener
{
    void onUpdate(Object... obj);
    
    void onError(Throwable error);
}
