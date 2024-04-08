package com.conney.keeptriple.local.net.exception;

import java.io.IOException;

public class DisconnectException extends IOException {

    public DisconnectException(String message)  {
        super(message + " disconnect");
    }
}
