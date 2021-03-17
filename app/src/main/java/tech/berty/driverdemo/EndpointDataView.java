package tech.berty.driverdemo;

public class EndpointDataView {
    String name;
    String id;
    String action;
    int sentMessages = 0;
    int receivedMessages = 0;

    public EndpointDataView(String name, String id, String action) {
        this.name = name;
        this.id = id;
        this.action = action;
    }

    public EndpointDataView(String id, String action) {
        this.id = id;
        this.action = action;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public int getSentMessages() {
        return sentMessages;
    }

    public int getReceivedMessages() {
        return receivedMessages;
    }

    public void incrementSentMessages() { sentMessages++; }

    public void incrementReceivedMessages() { receivedMessages++; }

    public void setAction(String action) {
        this.action = action;
    }
}