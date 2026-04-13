package model;

import java.io.Serializable;

/**
 * 记录最近一次操作说明，供HUD实时展示。
 */
public class OperationLog implements Serializable {
    private String lastMessage = "Ready";

    /**
     * 更新最近操作消息。
     */
    public void update(String message) {
        this.lastMessage = message;
    }

    /**
     * 返回最近操作消息。
     */
    public String lastMessage() {
        return lastMessage;
    }
}
