package com.example.back.model;

//ACTIVE, USED, CANCELLED
public enum TicketStatus {
    ACTIVE("active"),
    USED("used"),
    CANCELLED("cancelled");

    private String status;

    TicketStatus(String status){
        this.status = status;
    }

    public String getStatus(){
        return status;
    }

}
