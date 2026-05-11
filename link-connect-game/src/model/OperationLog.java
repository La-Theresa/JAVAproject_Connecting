package model;

import java.io.Serializable;

/**
 * 记录最近一次操作说明，供HUD实时展示。
 */
public class OperationLog implements Serializable {
    private String lastMessage = "Ready";

    public void update(String message) {
        this.lastMessage = message;
    }

    public String lastMessage() {
        return lastMessage;
    }
}
