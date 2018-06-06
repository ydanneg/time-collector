package io.ydanneg;

import static java.lang.System.exit;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.ydanneg.service.TimeCollectorService;

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
		System.out.println("args: " + Arrays.toString(args));
		Options options = new Options();

		Option input = new Option(OPT_PRINT, OPT_LONG_PRINT, false, OPT_PRINT_DESCRIPTION);
		input.setRequired(false);

		options.addOption(input);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
			log.trace("unknown command line parameters: {}", Arrays.toString(args));
			formatter.printHelp(HELP_TITLE, options);
			System.exit(1);
			return;
		}

		if (cmd.hasOption(OPT_LONG_PRINT)) {
			// TODO: start print service
			log.info("printing collected timestamps");
			timeCollectorService.test();
		} else {
			// TODO: collect and log timestamps
			log.info("starting time collector service");
			timeCollectorService.startService();
		}

		exit(0);
	}
}
