package io.ydanneg;

import static java.lang.System.exit;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.cli.CommandLine;
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
		SpringApplication app = new SpringApplication(SpringBootConsoleApplication.class);
		app.setBannerMode(Banner.Mode.OFF);
		app.run(args);
	}

	@Override
	public void run(String... args) {
		System.out.println("starting with args: " + Arrays.toString(args));

		final Option input = new Option(OPT_PRINT, OPT_LONG_PRINT, false, OPT_PRINT_DESCRIPTION);
		input.setRequired(false);

		final Options options = new Options();
		options.addOption(input);

		try {
			CommandLine cl = new DefaultParser().parse(options, args);
			if (cl.hasOption(OPT_LONG_PRINT)) {
				System.out.println("printing collected timestamps");
				timeCollectorService.print();
			} else {
				System.out.println("starting timestamp collector service");
				timeCollectorService.collect();
			}
			exit(0);
		} catch (ParseException e) {
			new HelpFormatter().printHelp(HELP_TITLE, options);
			exit(1);
		}
	}
}
