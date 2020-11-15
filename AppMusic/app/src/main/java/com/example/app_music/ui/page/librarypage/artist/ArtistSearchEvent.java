package com.fhm.musicr.ui.page.librarypage.artist;

public class ArtistSearchEvent {
    private String message = "";

    public ArtistSearchEvent() {
    }

    public ArtistSearchEvent(String message) {
        this.setMessage(message);
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
