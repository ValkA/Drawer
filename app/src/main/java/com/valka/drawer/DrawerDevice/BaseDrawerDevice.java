package com.valka.drawer.DrawerDevice;

import android.app.Activity;

import java.io.IOException;

/**
 * Created by valentid on 28/08/2017.
 */

public abstract class BaseDrawerDevice {
    public enum CommandResponse { OK, Error }

    public interface OnOpenListener{
        void onOpen(boolean success);
    }
    public interface OnCloseListener{
        void onClose(boolean success);
    }
    public interface OnGCodeCommandResponse{
        void onResponse(CommandResponse commandResponse);
    }
    abstract public void open(Activity activity, OnOpenListener onOpenListener);
    abstract public void close(Activity activity, OnCloseListener onCloseListener);
    abstract public void sendGCodeCommandASync(String gCodeCommand, OnGCodeCommandResponse onGCodeCommandResponse);
    abstract public CommandResponse sendGCodeCommand(String gCodeCommand);
}
