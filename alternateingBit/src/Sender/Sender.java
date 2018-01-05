package Sender;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Sender {

    private byte[] lastpacket;
    private String fileName;
    private int port;
    private String ip;
    private DatagramSocket ds;
    private int position = 0;
    private byte[] file;

//Filename,port,ip
public static void main(String args[]) throws IOException {
    String fileName = args[0];
    int port = Integer.parseInt(args[1]);
    String ip = args[2];
    Path file = Paths.get(fileName);
    byte[] data = Files.readAllBytes(file);
    Sender s = new Sender(fileName,ip,port,data);


}
    // all states for this FSM
    enum State {
        IDLE, WAIT0, WAIT1, Wait0END, Wait1END,END
    }

    ;

    // all messages/conditions which can occur
//    enum Msg {
//        SEND_FILENAME, ACK0STAY, ACK0TIMEOUT, ACK1SENDLASTDATA, ACK1SENDDATA, ACK1STAY, ACK1TIMEOUT, ACK0SENDLASTDATA, ACK0SENDDATA,
//        ACK1END,ACK1ENDSTAY,ACK1ENDTIMEOUT,ACK0END,ACK0ENDSTAY,ACK0ENDTIMEOUT
//    }

        enum Msg {
        SEND_FILENAME, STAY, SEND_AGAIN, SENDLASTDATA, SENDDATA
    }


    // current state of the FSM
    private State currentState;
    // 2D array defining all transitions that can occur
    private Transition[][] transition;

    /**
     * constructor
     */
    public Sender(String fileName,String ip, int port,byte[] file) throws SocketException {
        this.port = port;
        this.fileName = fileName;
        this.ip = ip;
        this.file = file;
        currentState = State.IDLE;
        // define all valid state transitions for our state machine
        // (undefined transitions will be ignored)
        transition = new Transition[State.values().length][Msg.values().length];
        //fist
        transition[State.IDLE.ordinal()][Msg.SEND_FILENAME.ordinal()] = new SendFilename();
        //wait0
        transition[State.WAIT0.ordinal()][Msg.STAY.ordinal()] = new stayACK0();
        transition[State.WAIT0.ordinal()][Msg.SEND_AGAIN.ordinal()] = new timeout0();
        transition[State.WAIT0.ordinal()][Msg.SENDDATA.ordinal()] = new ACK0send();
        transition[State.WAIT0.ordinal()][Msg.SENDLASTDATA.ordinal()] = new ACK0sendLast();
        //wait1
        transition[State.WAIT1.ordinal()][Msg.STAY.ordinal()] = new stayACK1();
        transition[State.WAIT1.ordinal()][Msg.SEND_AGAIN.ordinal()] = new timeout1();
        transition[State.WAIT1.ordinal()][Msg.SENDDATA.ordinal()] = new ACK0send();
        transition[State.WAIT1.ordinal()][Msg.SENDLASTDATA.ordinal()] = new ACK0sendLast();
        //waitACK0end
        transition[State.Wait0END.ordinal()][Msg.STAY.ordinal()] = new End0stay();
        transition[State.Wait0END.ordinal()][Msg.SEND_AGAIN.ordinal()] = new timeoutend0();
        transition[State.Wait0END.ordinal()][Msg.SENDDATA.ordinal()] = new End0end();
        //waitACK1end
        transition[State.Wait1END.ordinal()][Msg.STAY.ordinal()] = new End1stay();
        transition[State.Wait1END.ordinal()][Msg.SEND_AGAIN.ordinal()] = new timeoutend1();
        transition[State.Wait1END.ordinal()][Msg.SENDDATA.ordinal()] = new End1end();




        System.out.println("INFO FSM constructed, current state: " + currentState);


        ds = new DatagramSocket();
        ds.setSoTimeout(500);
        processMsg(Msg.SEND_FILENAME);

        while (currentState != State.END) {
            waitAck();
        }
    }


    private void waitAck() {
        DatagramPacket inPacket = new DatagramPacket(new byte[512], 512);
        try {
            ds.receive(inPacket);
            byte[] packetData = inPacket.getData();
            if(PACKET.isCorrupt(packetData)){
                processMsg(Msg.SEND_AGAIN); //corrupt
            }else if(!PACKET.isCorrupt(packetData) &&(PACKET.getAck(packetData)!=PACKET.getAck(lastpacket))){
                processMsg(Msg.SEND_AGAIN);//nicht corrupt aber falsches ack
            }
            else if(!PACKET.isCorrupt(packetData) &&(PACKET.getAck(packetData)!=PACKET.getAck(lastpacket))){
                if(file.length - position < 490){
                    processMsg(Msg.SENDLASTDATA); //last data
                }else {
                    processMsg(Msg.SENDDATA);
                }
            }
        } catch (SocketTimeoutException e) {
            processMsg(Msg.SEND_AGAIN); //timeout

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Process a message (a condition has occurred).
     *
     * @param input Message or condition that has occurred.
     */
    public void processMsg(Msg input) {
        System.out.println("INFO Received " + input + " in state " + currentState);
        Transition trans = transition[currentState.ordinal()][input.ordinal()];
        if (trans != null) {
            currentState = trans.execute(input);
        }
        System.out.println("INFO State: " + currentState);
    }

    /**
     * Abstract base class for all transitions.
     * Derived classes need to override execute thereby defining the action
     * to be performed whenever this transition occurs.
     */
    abstract class Transition {
        abstract public State execute(Msg input);
    }

    class SendFilename extends Transition {
        @Override
        public State execute(Msg input) {
            byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);
            byte [] data = PACKET.createPacket(false,nameBytes,false);
            sendData(data);
            System.out.println("SendFilename");

            return State.WAIT0;

        }
    }

    class stayACK0 extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("");
            return State.WAIT0;
        }
    }

    class timeout0 extends Transition {
        @Override
        public State execute(Msg input) {
            sendData(lastpacket);
            System.out.println("timeout send 0 again");
            return State.WAIT0;
        }
    }

    class ACK0send extends Transition {
        @Override
        public State execute(Msg input) {
            byte[] p = new byte[490];
            System.arraycopy(file,position,p,0,490);
            position +=490;
            byte [] data = PACKET.createPacket(true,p,false);
            sendData(data);
            System.out.println("send data 1");
            return State.WAIT1;
        }
    }

    class ACK0sendLast extends Transition {
        @Override
        public State execute(Msg input) {
            byte[] p = new byte[490];
            System.arraycopy(file, position, p, 0, file.length - position); //letzte daten länge berechenen
            byte [] data = PACKET.createPacket(true, p, false);
            sendData(data);
            System.out.println("send last data 1");
            return State.Wait1END;

        }
    }

    class stayACK1 extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("");
            return State.WAIT1;
        }
    }

    class timeout1 extends Transition {
        @Override
        public State execute(Msg input) {
            sendData(lastpacket);
            System.out.println("timeout send 1 again");
            return State.WAIT1;
        }
    }

    class ACK1send extends Transition {
        @Override
        public State execute(Msg input) {
            byte[] p = new byte[490];
            System.arraycopy(file,position,p,0,490);
            position +=490;
            byte [] data = PACKET.createPacket(false,p,false);
            sendData(data);
            System.out.println("send Data 0");
            return State.WAIT0;
        }
    }

    class ACK1sendLast extends Transition {
        @Override
        public State execute(Msg input) {
            byte[] p = new byte[490];
            System.arraycopy(file, position, p, 0, file.length - position); //letzte daten länge berechenen
            byte [] data = PACKET.createPacket(true, p, false);
            sendData(data);
            System.out.println("send last data 1");
            return State.Wait0END;
        }
    }

    class timeoutend0 extends Transition {
        @Override
        public State execute(Msg input) {
            sendData(lastpacket);
            System.out.println("timeout send last 0 again");
            return State.Wait0END;
        }
    }

    class End0stay extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("");
            return State.Wait0END;
        }
    }

    class End0end extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("");
            return State.END;
        }
    }

    class timeoutend1 extends Transition {
        @Override
        public State execute(Msg input) {
            sendData(lastpacket);
            System.out.println("timeout send last 1 again");
            return State.Wait1END;
        }
    }

    class End1stay extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("");
            return State.Wait1END;
        }
    }

    class End1end extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("");
            return State.END;
        }
    }

        private void sendData(byte [] data)  {
            try {

                InetAddress iadress = InetAddress.getByName(ip);

                DatagramPacket dp = new DatagramPacket(data, data.length, iadress, port);
                ds.send(dp);
                lastpacket = data;
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


}