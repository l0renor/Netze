package Sender;

public class Sender {
//Filename,port,ip
public static void main(String args[]){
    String filename = args[0];
    int port = Integer.parseInt(args[1]);
    String ip = args[2];
    Sender s = new Sender();


}
    /**
     * Finate State Machine (FSM) Java Example: Woman
     * (lecture Slides for first lecture, p. 19)
     */
    // all states for this FSM
    enum State {
        IDLE, WAIT0, WAIT1, Wait0END, Wait1END
    }

    ;

    // all messages/conditions which can occur
    enum Msg {
        SEND_FILENAME, ACK0STAY, ACK0TIMEOUT, ACK1SENDLASTDATA, ACK1SENDDATA, ACK1STAY, ACK1TIMEOUT, ACK0SENDLASTDATA, ACK0SENDDATA,
        ACK1END,ACK1ENDSTAY,ACK1ENDTIMEOUT,ACK0END,ACK0ENDSTAY,ACK0ENDTIMEOUT
    }


    // current state of the FSM
    private State currentState;
    // 2D array defining all transitions that can occur
    private Transition[][] transition;

    /**
     * constructor
     */
    public Sender() {
        currentState = State.IDLE;
        // define all valid state transitions for our state machine
        // (undefined transitions will be ignored)
        transition = new Transition[State.values().length][Msg.values().length];

        transition[State.IDLE.ordinal()][Msg.SEND_FILENAME.ordinal()] = new SendFilename();

        transition[State.WAIT0.ordinal()][Msg.ACK0STAY.ordinal()] = new stayACK0();
        transition[State.WAIT0.ordinal()][Msg.ACK0TIMEOUT.ordinal()] = new timeout0();
        transition[State.WAIT0.ordinal()][Msg.ACK0SENDDATA.ordinal()] = new ACK0send();
        transition[State.WAIT0.ordinal()][Msg.ACK0SENDDATA.ordinal()] = new ACK0sendLast();

        transition[State.WAIT1.ordinal()][Msg.ACK1STAY.ordinal()] = new stayACK0();
        transition[State.WAIT1.ordinal()][Msg.ACK1TIMEOUT.ordinal()] = new timeout0();
        transition[State.WAIT1.ordinal()][Msg.ACK1SENDDATA.ordinal()] = new ACK0send();
        transition[State.WAIT1.ordinal()][Msg.ACK1SENDDATA.ordinal()] = new ACK0sendLast();




        System.out.println("INFO FSM constructed, current state: " + currentState);
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
            System.out.println("");
            return State.WAIT0;
        }
    }

    class ACK0send extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("");
            return State.WAIT1;
        }
    }

    class ACK0sendLast extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("");
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
            System.out.println("");
            return State.WAIT1;
        }
    }

    class ACK1send extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("");
            return State.WAIT0;
        }
    }

    class ACK1sendLast extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("");
            return State.Wait0END;
        }
    }

    class Finish extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("Thank you.");
            return State.IDLE;
        }
    }

}