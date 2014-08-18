/*	
 * Class 	ATManager
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */
package general;

import com.cinterion.io.*;

public class ATManager implements ATCommandListener, ATCommandResponseListener {

    private ATCommand atCommand;

    public ATManager() {
        try {
            atCommand = new ATCommand(true);
            atCommand.addListener(this);
        } catch (ATCommandFailedException atcfe) {
            System.err.println("ATCommandFailedException new ATCommand");
        }
    }

    public static ATManager getInstance() {
        return ModemManagerHolder.INSTANCE;
    }

    private static class ModemManagerHolder {

        private static final ATManager INSTANCE = new ATManager();
    }

    public String executeCommandSynchron(String command) {
        return execute(command, null, null);
    }

    public String executeCommandSynchron(String command, String text) {
        return execute(command, text, null);
    }

    public void executeCommand(String command) {
        String response = execute(command, null, this);
    }

    private synchronized String execute(String command, String text, ATCommandResponseListener listener) {
        String response = "";

        if (Settings.getInstance().getSetting("gsmDebug", false)) {
            System.out.println("execute Command: " + command);
            System.out.flush();
        }
        try {
            if (listener == null) {
                response = atCommand.send(command);
                if (Settings.getInstance().getSetting("gsmDebug", false)) {
                    System.out.println("commandResponse: " + response);
                    System.out.flush();
                }
                if (text != null) {
                    response = response + atCommand.send(text + "\032");
                }
            } else {
                atCommand.send(command, listener);
            }
        } catch (ATCommandFailedException atcfe) {
            new LogError("ATCommandFailedException send " + command);
        }

        return response;
    }

    public void ATEvent(String event) {
        if (event == null) {
            return;
        }

        if (Settings.getInstance().getSetting("gsmDebug", false)) {
            System.out.println("ATListenerEvents: " + event);
        }

        if (event.indexOf("^SYSSTART AIRPLANE MODE") >= 0) {
            AppMain.getInstance().airplaneMode = true;
        }

        if (event.indexOf("+CGREG") >= 0) {
            try {
                SocketGPRSThread.getInstance().cgreg = Integer.parseInt(
                        event.substring((event.indexOf(": ")) + 2,
                                (event.indexOf(": ")) + 3)
                );
            } catch (NumberFormatException nfe) {
                SocketGPRSThread.getInstance().cgreg = -1;
            }
        }

        if (event.indexOf("+CREG") >= 0) {
            try {
                SocketGPRSThread.getInstance().creg = Integer.parseInt(
                        event.substring((event.indexOf(": ")) + 2,
                                (event.indexOf(": ")) + 3));
            } catch (NumberFormatException nfe) {
                SocketGPRSThread.getInstance().creg = -1;
            }
        }

        if (event.indexOf("^SCPOL: ") >= 0) {
            GPIOInputManager.getInstance().eventGPIOValueChanged(event);
        }

        if (event.indexOf("+CMTI: ") >= 0) {
            ProcessSMSThread.eventSMSArrived(event);
        }

        if (event.indexOf("^SBC: Undervoltage") >= 0) {
            BatteryManager.getInstance().eventLowBattery();
        }

        if (event.indexOf("^SCKS") >= 0) {
            new LogError(event);
            if (event.indexOf("2") >= 0) {
                AppMain.getInstance().invalidSIM = true;
            }
        }
    }

    public void CONNChanged(boolean SignalState) {
    }

    public void RINGChanged(boolean SignalState) {
    }

    public void DCDChanged(boolean SignalState) {
    }

    public void DSRChanged(boolean SignalState) {
    }

    public void ATResponse(String response) {
        if (Settings.getInstance().getSetting("gsmDebug", false)) {
            System.out.println("commandResponse: " + response);
        }
    }
}
