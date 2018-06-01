package io.ydanneg;

import com.mkyong.service.HelloMessageService;
import io.ydanneg.service.TimeCollectorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

import static java.lang.System.exit;

@SpringBootApplication
@Slf4j
public class SpringBootConsoleApplication implements CommandLineRunner {

    private static final String HELP_TITLE = "time-collector";
    private static final String OPT_PRINT = "p";
    private static final String OPT_LONG_PRINT = "print";
    private static final String OPT_PRINT_DESCRIPTION = "print collected time";

    @Autowired
    private TimeCollectorService timeCollectorService;

    public static void main(String[] args) {

        //disabled banner, don't want to see the spring logo
        SpringApplication app = new SpringApplication(SpringBootConsoleApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);

        //SpringApplication.run(SpringBootConsoleApplication.class, args);
    }

    @Override
    public void run(String... args) {
        Options options = new Options();

        Option input = new Option(OPT_PRINT, OPT_LONG_PRINT, true, OPT_PRINT_DESCRIPTION);
        input.setRequired(false);

        options.addOption(input);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            log.trace("unknown command line parameters: {}", Arrays.toString(args));
            formatter.printHelp(HELP_TITLE, options);
            System.exit(1);
            return;
        }

        if (cmd.hasOption(OPT_LONG_PRINT)) {
            // TODO: start print service
            log.info("printing collected timestamps");
        } else {
            // TODO: collect and log timestamps
            log.info("starting time collector service");
            timeCollectorService.startService();
        }

        exit(0);
    }
}
