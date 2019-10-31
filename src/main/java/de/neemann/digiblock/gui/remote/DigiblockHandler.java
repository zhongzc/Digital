/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.gui.remote;

import de.neemann.digiblock.gui.DigiblockRemoteInterface;
import de.neemann.digiblock.lang.Lang;

import java.io.File;

/**
 * Handler to control the simulator.
 * The handler simply interprets the incoming request and calls the suited method
 * of the {@link DigiblockRemoteInterface} which is implemented by the {@link de.neemann.digiblock.gui.Main} class.
 */
public class DigiblockHandler implements HandlerInterface {
    private final DigiblockRemoteInterface digiblockRemoteInterface;

    /**
     * Creates a new server instance
     *
     * @param digiblockRemoteInterface the remote interface which is used by the server
     */
    public DigiblockHandler(DigiblockRemoteInterface digiblockRemoteInterface) {
        this.digiblockRemoteInterface = digiblockRemoteInterface;
    }

    @Override
    public String handleRequest(String request) {
        int p = request.indexOf(':');
        String command = request;
        String args = null;
        if (p >= 0) {
            command = request.substring(0, p);
            args = request.substring(p + 1);
        }

        try {
            String ret = handle(command.toLowerCase(), args);
            if (ret != null)
                return "ok:"+ret;
            else
                return "ok";
        } catch (RemoteException e) {
            return e.getMessage();
        }
    }

    private String handle(String command, String args) throws RemoteException {
        switch (command) {
            case "step":
                return digiblockRemoteInterface.doSingleStep();
            case "start":
                digiblockRemoteInterface.start(new File(args));
                return null;
            case "debug":
                digiblockRemoteInterface.debug(new File(args));
                return null;
            case "run":
                return digiblockRemoteInterface.runToBreak();
            case "stop":
                digiblockRemoteInterface.stop();
                return null;
            default:
                throw new RemoteException(Lang.get("msg_remoteUnknownCommand", command));
        }
    }
}
