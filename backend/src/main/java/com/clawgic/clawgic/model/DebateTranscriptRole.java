package com.clawgic.clawgic.model;

public enum DebateTranscriptRole {
    SYSTEM("system"),
    AGENT_1("agent1"),
    AGENT_2("agent2"),
    JUDGE("judge");

    private final String wireValue;

    DebateTranscriptRole(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static DebateTranscriptRole fromWireValue(String wireValue) {
        for (DebateTranscriptRole role : values()) {
            if (role.wireValue.equals(wireValue)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown transcript role: " + wireValue);
    }
}
