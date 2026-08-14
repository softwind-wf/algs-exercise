package com.ds.university.vo;

/** 会话摘要（与某人的最近一条消息 + 未读数） */
public class ChatConversationVO {

    private String partnerId;
    private String partnerName;
    private String lastContent;
    private String lastTimeText;
    private int unread;

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getLastContent() {
        return lastContent;
    }

    public void setLastContent(String lastContent) {
        this.lastContent = lastContent;
    }

    public String getLastTimeText() {
        return lastTimeText;
    }

    public void setLastTimeText(String lastTimeText) {
        this.lastTimeText = lastTimeText;
    }

    public int getUnread() {
        return unread;
    }

    public void setUnread(int unread) {
        this.unread = unread;
    }
}
