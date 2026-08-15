package nro.models.network;

import java.net.Socket;
import java.io.DataInputStream;
import java.io.IOException;
import nro.models.interfaces.IMessageHandler;
import nro.models.interfaces.IMessageSendCollect;
import nro.models.interfaces.ISession;
import nro.models.utils.Logger;

public final class Collector
        implements Runnable {

    private ISession session;
    private DataInputStream dis;
    private IMessageSendCollect collect;
    private IMessageHandler messageHandler;

    public Collector(ISession session, Socket socket) {
        this.session = session;
        this.setSocket(socket);
    }

    public Collector setSocket(Socket socket) {
        try {
            this.dis = new DataInputStream(socket.getInputStream());
        } catch (IOException iOException) {
        }
        return this;
    }

    @Override
    public void run() {
        try {
            while (this.session != null && this.session.isConnected()) {
                Message msg = this.collect.readMessage(this.session, this.dis);
                boolean closeConnection = false;
                try {
                    if (!(this.session instanceof MySession)) {
                        closeConnection = true;
                    } else {
                        MySession mySession = (MySession) this.session;
                        if (!this.session.sentKey()) {
                            if (msg.command != -27) {
                                Logger.warning("[CLIENT ACCESS] " + mySession.ipAddress
                                        + " sent command " + msg.command + " before -27 handshake\n");
                                closeConnection = true;
                            } else if (!ClientAccessAuth.authenticate(mySession, msg)) {
                                closeConnection = true;
                            } else {
                                this.session.sendKey();
                                closeConnection = !this.session.sentKey();
                            }
                        } else if (msg.command == -27) {
                            Logger.warning("[CLIENT ACCESS] " + mySession.ipAddress
                                    + " sent a duplicate -27 handshake\n");
                            closeConnection = true;
                        } else if (!mySession.accessVerified) {
                            Logger.warning("[CLIENT ACCESS] " + mySession.ipAddress
                                    + " has no verified handshake state\n");
                            closeConnection = true;
                        } else {
                            this.messageHandler.onMessage(this.session, msg);
                        }
                    }
                } finally {
                    msg.cleanup();
                }
                if (closeConnection) {
                    break;
                }
            }
        } catch (Exception exception) {
        }
        try {
            Network.gI().getAcceptHandler().sessionDisconnect(this.session);
        } catch (Exception exception) {
        }
        if (this.session != null) {
            this.session.disconnect();
        }
    }

    public void setCollect(IMessageSendCollect collect) {
        this.collect = collect;
    }

    public void setMessageHandler(IMessageHandler handler) {
        this.messageHandler = handler;
    }

    public void close() {
        if (this.dis != null) {
            try {
                this.dis.close();
            } catch (IOException iOException) {
            }
        }
    }

    public void dispose() {
        this.session = null;
        this.dis = null;
        this.collect = null;
    }
}
