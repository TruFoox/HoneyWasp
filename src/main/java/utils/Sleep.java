package utils;

import services.Services;

public class Sleep {

    public static void milliseconds(Services service, long milliseconds) throws Exception {
        if (service != null) {service.sleeping = true;}
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
            if (service != null) {service.sleeping = false;}
            throw new InterruptedException(e.getMessage());
        }
        if (service != null) {service.sleeping = false;}
    }
}