package utils;

public class Sleep { // Prevents the bot from sleeping if there is a real interruption (like attempted exit)
    public static boolean safeSleep(long ms) throws Exception {
        try {
            Thread.sleep(ms);

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt flag

            return false;
        }
    }
}
