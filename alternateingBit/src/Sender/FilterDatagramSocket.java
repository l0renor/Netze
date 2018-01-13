package Sender;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class FilterDatagramSocket extends DatagramSocket {

  private final double LOSS;
  private final double DUPLICATE;
  private final double CORRUPT;

  public FilterDatagramSocket(int port, double loss, double duplicate, double corrupt) throws SocketException {
    super(port);
    this.CORRUPT = corrupt;
    this.LOSS = loss;
    this.DUPLICATE = duplicate;
  }

  @Override
  public void send(DatagramPacket p) throws IOException {
    int timesToSend = 1;
    if(Math.random() > LOSS) {
      if(Math.random() < DUPLICATE) {
        timesToSend = 2;
      }
      for(int i = 0; i < timesToSend; i++) {
        if(Math.random() < CORRUPT) {
          byte[] packetData = p.getData();
          int arrayNumber = (int) (Math.random()*498)+14;
          packetData[arrayNumber] += 1;
          p.setData(packetData);
          super.send(p);
        }
      }
    }
  }
}
