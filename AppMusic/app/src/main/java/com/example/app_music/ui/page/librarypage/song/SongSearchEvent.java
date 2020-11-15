package com.fhm.musicr.ui.page.librarypage.song;

public class SongSearchEvent {
    private String message = "";

    public SongSearchEvent() {
    }

    public SongSearchEvent(String message) {
        this.setMessage(message);
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
