package com.flyn.net.wifi.direct;

public class NameOutOfRangeException extends Exception
{
    private static final long serialVersionUID = 1L;

    public NameOutOfRangeException()
    {
    }

    public NameOutOfRangeException(String message)
    {
        super(message);
    }

    public NameOutOfRangeException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public NameOutOfRangeException(Throwable cause)
    {
        super(cause);
    }
}
