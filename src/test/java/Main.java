import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: medi0cr1ty
 * @date: 2023/11/1
 * @time: 15:08
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.debug("This is a debug message.");
        logger.info("This is an info message.");
        logger.warn("This is a warning message.");
        logger.error("This is an error message.");
    }
}
