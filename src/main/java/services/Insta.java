package services;

import config.Config;
import config.InstagramSettings;
import services.Services;
import utils.Output;

public class Insta extends Services implements HasUserID {
    public Insta() {
        super("Instagram","INSTA",Config.getInstance());
        settings = config.Platform("instagram"); // Establish settings\

        InstagramSettings ig = Config.getInstance().Instagram();
        TOKEN = ig.getApi_key();
        DESCRIPTION = ig.getCaption();

    }
    public boolean fetchUserID() {

        return false;
    }
    protected boolean upload() {
        Output.debugPrint("e");
        return false;
    }
    protected boolean publish() {

        return false;
    }
    protected boolean fetchUserToken() {

        return false;
    }
}