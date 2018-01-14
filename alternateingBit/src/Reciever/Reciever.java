package Reciever;

import Sender.PACKET;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Reciever {

  public static void main(String args[]) throws IOException {
    int port = 8799;
    Reciever r = new Reciever(port);
  }

  private String filename;
  private int port;
  private int senderport;
  private DatagramSocket ds;
  private byte[] file;
  private InetAddress senderinetadress;
  private boolean lastACK;

  private static final String PATH = "F:\\Dokumente\\Uni\\";


  enum State {
    IDLE, WAIT1, WAIT0, END
  }

  enum Msg {
    SEND_ACK, STAY, SEND_LAST_ACK
  }

  private State currentState;

  private Transition[][] transition;

  public Reciever(int port) throws SocketException {
    this.port = port;
    this.file = new byte[0];
    currentState = State.IDLE;
    lastACK = true;

    transition = new Transition[State.values().length][Msg.values().length];
    //idle
    transition[State.IDLE.ordinal()][Msg.SEND_ACK.ordinal()] = new RcvFilename();
    transition[State.IDLE.ordinal()][Msg.STAY.ordinal()] = new stayIdle();
    //wait0
    transition[State.WAIT0.ordinal()][Msg.STAY.ordinal()] = new stayWait0();
    transition[State.WAIT0.ordinal()][Msg.SEND_ACK.ordinal()] = new sendACK0();
    transition[State.WAIT0.ordinal()][Msg.SEND_LAST_ACK.ordinal()] = new sendLastACK0();
    //wait1
    transition[State.WAIT1.ordinal()][Msg.STAY.ordinal()] = new stayWait1();
    transition[State.WAIT1.ordinal()][Msg.SEND_ACK.ordinal()] = new sendACK1();
    transition[State.WAIT1.ordinal()][Msg.SEND_LAST_ACK.ordinal()] = new sendLastACK1();

    System.out.println("INFO FSM constructed, current state: " + currentState);

    while (currentState != State.END) {
      waitData();
    }

  }

  private void waitData() {
    DatagramPacket inPacket = new DatagramPacket(new byte[512], 512);
    try {
      ds.receive(inPacket);
      byte[] packetData = inPacket.getData();
      senderport = inPacket.getPort();
      senderinetadress = inPacket.getAddress();
      if (PACKET.isCorrupt(packetData)) {
        processMsg(Msg.STAY); //corrupt
        System.out.println("Corrupt");
      } else if (PACKET.getAck(packetData) == lastACK) {
        processMsg(Msg.STAY); //wrong seq nr
        System.out.println("Wrong nr"+PACKET.getAck(packetData)+lastACK);
      } else {
        int size = PACKET.getSize(packetData);
        byte[] newfile = new byte[file.length + size];
        System.arraycopy(file, 0, newfile, 0, file.length);
        System.arraycopy(PACKET.getContent(packetData), 0, newfile, file.length, size);
        file = newfile;
        if (PACKET.isEnd(packetData)) {
          processMsg(Msg.SEND_LAST_ACK); //last packet got
        } else {
          processMsg(Msg.SEND_ACK);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void processMsg(Msg input) {
    System.out.println("INFO Received " + input + " in state " + currentState);
    Transition trans = transition[currentState.ordinal()][input.ordinal()];
    if (trans != null) {
      currentState = trans.execute(input);
    }
    System.out.println("INFO State: " + currentState);
  }

  abstract class Transition {

    abstract public State execute(Msg input);
  }

  class RcvFilename extends Transition {
    @Override
    public State execute(Msg input) {
      filename = new String(file);
      file = new byte[0];
      System.out.println("Filename recieved:"+filename);
      byte[] data = PACKET.createPacket(false, new byte[0], false);
      sendData(data);
      lastACK = false;
      System.out.println("Acknowledge (0) for filename sent");
      return State.WAIT1;
    }
  }

  class stayIdle extends Transition {
    @Override
    public State execute(Msg input) {
      System.out.println("Staying in idle");
      return State.IDLE;
    }
  }

  class stayWait0 extends Transition {
    @Override
    public State execute(Msg input) {
        byte[] data = PACKET.createPacket(true, new byte[0], false);
        sendData(data);
        lastACK = true;
      System.out.println("Staying in 0");
      return State.WAIT0;
    }
  }

  class sendACK0 extends Transition {
    @Override
    public State execute(Msg input) {
      byte[] data = PACKET.createPacket(false, new byte[0], false);
      sendData(data);
      lastACK = false;
      System.out.println("Acknowledge 0 sent");
      return State.WAIT1;
    }
  }

  class sendLastACK0 extends Transition {
    @Override
    public State execute(Msg input) {
      byte[] data = PACKET.createPacket(false, new byte[0], false);
      sendData(data);
      lastACK = false;
      createFile();
      System.out.println("Last Acknowledge 0 sent");
      createFile();
      return State.END;
    }
  }

  class stayWait1 extends Transition {
    @Override
    public State execute(Msg input) {
        byte[] data = PACKET.createPacket(false, new byte[0], false);
        sendData(data);
        lastACK = false;
      System.out.println("Staying in 0");
      return State.WAIT1;
    }
  }

  class sendACK1 extends Transition {
    @Override
    public State execute(Msg input) {
      byte[] data = PACKET.createPacket(true, new byte[0], false);
      sendData(data);
      lastACK = true;
      System.out.println("Acknowledge 1 sent");
      return State.WAIT0;
    }
  }

  class sendLastACK1 extends Transition {
    @Override
    public State execute(Msg input) {
      byte[] data = PACKET.createPacket(true, new byte[0], false);
      sendData(data);
      lastACK = true;
      System.out.println("Last Acknowledge 1 sent");
      createFile();
      return State.END;
    }
  }

  private boolean createFile() {
    try {
      Path filePath = Files.createFile(Paths.get("F:\\Dokumente\\Uni\\test.pdf"));
      Files.write(filePath, file);
      System.out.println("File has been transferred and created at " + PATH);
      return true;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  private void sendData(byte[] data) {
    try {
      DatagramPacket dp = new DatagramPacket(data, data.length, senderinetadress, senderport);
      ds.send(dp);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
