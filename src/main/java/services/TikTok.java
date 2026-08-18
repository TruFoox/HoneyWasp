package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.HoneyWasp;
import org.json.JSONObject;
import utils.FileIO;
import utils.HTTPSend;
import utils.Output;
import utils.StringToJson;

import java.awt.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TikTok extends Services implements HasRefreshToken {
    private final String CLIENT_KEY, SECRET;

    public TikTok() {
        super("TikTok","TT");

        SECRET = HoneyWasp.config.Tiktok().getClient_secret().trim();
        CLIENT_KEY = HoneyWasp.config.Tiktok().getClient_key().trim();
        REFRESH_TOKEN = HoneyWasp.config.Tiktok().getRefresh_token().trim();
        VIDEO_MODE = true; // TikTok technically supports images, but I just don't want to bother
        supportedAspectRatio = new double[]{0.82, 1.70}; // ~4:5 to 17:10
    }

    @Override
    public boolean fetchRefreshToken() {

        return true;
    }

    @Override
    protected boolean upload() throws Exception {

        return true;
    }

    @Override
    protected boolean publish() throws Exception {

        return true;
    }

    @Override
    protected boolean fetchUserToken() throws Exception {

        return true;
    }
}
