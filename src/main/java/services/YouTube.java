package services;

import config.ReadConfig;
import utils.DateTime;
import utils.HTTPSend;
import utils.Output;
import utils.StringToJson;
import services.Instagram;
import services.YouTube;


public class YouTube implements Runnable {
    ReadConfig config = ReadConfig.getInstance(); // Get config

    public void run() {
        String token = config.getInstagram().getApi_key().trim();
    }
}