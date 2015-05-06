/*
 * Copyright (C) 2015 Julien Bonjean <julien@bonjean.info>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package info.bonjean.quinkana;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * To keep it light, every error is fatal, and we don't use a logging library.
 *
 * @author Julien Bonjean <julien@bonjean.info>
 *
 */
public class Main {
	private enum Action {
		tail, list;
	}

	private static void run(String[] args) throws QuinkanaException {
		Properties properties = new Properties();
		InputStream inputstream = Main.class.getResourceAsStream("/config.properties");

		try {
			properties.load(inputstream);
			inputstream.close();
		} catch (IOException e) {
			throw new QuinkanaException("cannot load internal properties", e);
		}

		String name = (String) properties.get("name");
		String description = (String) properties.get("description");
		String url = (String) properties.get("url");
		String version = (String) properties.get("version");
		String usage = (String) properties.get("help.usage");
		String defaultHost = (String) properties.get("default.host");
		int defaultPort = Integer.valueOf(properties.getProperty("default.port"));

		ArgumentParser parser = ArgumentParsers.newArgumentParser(name).description(description)
				.epilog("For more information, go to " + url).version("${prog} " + version).usage(usage);
		parser.addArgument("ACTION").type(Action.class).choices(Action.tail, Action.list).dest("action");
		parser.addArgument("-H", "--host").setDefault(defaultHost)
				.help(String.format("logstash host (default: %s)", defaultHost));
		parser.addArgument("-P", "--port").type(Integer.class).setDefault(defaultPort)
				.help(String.format("logstash TCP port (default: %d)", defaultPort));
		parser.addArgument("-f", "--fields").nargs("+").help("fields to display");
		parser.addArgument("-i", "--include").nargs("+").help("include filter (OR), example host=example.com")
				.metavar("FILTER").type(Filter.class);
		parser.addArgument("-x", "--exclude").nargs("+")
				.help("exclude filter (OR, applied after include), example: severity=debug").metavar("FILTER")
				.type(Filter.class);
		parser.addArgument("-s", "--single").action(Arguments.storeTrue()).help("display single result and exit");
		parser.addArgument("--version").action(Arguments.version()).help("output version information and exit");

		Namespace ns = parser.parseArgsOrFail(args);

		Action action = ns.get("action");
		List<String> fields = ns.getList("fields");
		List<Filter> includes = ns.getList("include");
		List<Filter> excludes = ns.getList("exclude");
		boolean single = ns.getBoolean("single");
		String host = ns.getString("host");
		int port = ns.getInt("port");

		final Socket clientSocket;
		final InputStream is;
		try {
			clientSocket = new Socket(host, port);
			is = clientSocket.getInputStream();
		} catch (IOException e) {
			throw new QuinkanaException("cannot connect to the server " + host + ":" + port, e);
		}

		// add a hook to ensure we clean the resources when leaving
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					clientSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		// prepare JSON parser
		ObjectMapper mapper = new ObjectMapper();
		JsonFactory jsonFactory = mapper.getFactory();
		JsonParser jp;
		try {
			jp = jsonFactory.createParser(is);
		} catch (IOException e) {
			throw new QuinkanaException("error during JSON parser creation", e);
		}

		JsonToken token;

		// action=list
		if (action.equals(Action.list)) {
			try {
				// do this in a separate loop to not pollute the main loop
				while ((token = jp.nextToken()) != null) {
					if (token != JsonToken.START_OBJECT)
						continue;

					// parse object
					JsonNode node = jp.readValueAsTree();

					// print fields
					Iterator<String> fieldNames = node.fieldNames();
					while (fieldNames.hasNext())
						System.out.println(fieldNames.next());

					System.exit(0);
				}
			} catch (IOException e) {
				throw new QuinkanaException("error during JSON parsing", e);
			}
		}

		// action=tail
		try {
			while ((token = jp.nextToken()) != null) {
				if (token != JsonToken.START_OBJECT)
					continue;

				// parse object
				JsonNode node = jp.readValueAsTree();

				// filtering (includes)
				if (includes != null) {
					boolean skip = true;
					for (Filter include : includes) {
						if (include.match(node)) {
							skip = false;
							break;
						}
					}
					if (skip)
						continue;
				}

				// filtering (excludes)
				if (excludes != null) {
					boolean skip = false;
					for (Filter exclude : excludes) {
						if (exclude.match(node)) {
							skip = true;
							break;
						}
					}
					if (skip)
						continue;
				}

				// if no field specified, print raw output (JSON)
				if (fields == null) {
					System.out.println(node.toString());
					if (single)
						break;
					continue;
				}

				// formatted output, build and print the string
				StringBuilder sb = new StringBuilder(128);
				for (String field : fields) {
					if (sb.length() > 0)
						sb.append(" ");

					if (node.get(field) != null && node.get(field).textValue() != null)
						sb.append(node.get(field).textValue());
				}
				System.out.println(sb.toString());

				if (single)
					break;
			}
		} catch (IOException e) {
			throw new QuinkanaException("error during JSON parsing", e);
		}
	}

	public static void main(String[] args) {
		try {
			run(args);
		} catch (QuinkanaException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
}
