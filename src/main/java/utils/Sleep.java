package utils;

import services.Services;

public class Sleep {

    public static void milliseconds(Services service, long milliseconds) throws Exception {
        service.sleeping = true;
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
            service.sleeping = false;
            throw new InterruptedException(e.getMessage());
        }
        service.sleeping = false;
    }
}