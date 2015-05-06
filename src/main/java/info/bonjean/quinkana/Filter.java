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

import java.io.Serializable;

import net.sourceforge.argparse4j.inf.ArgumentParserException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 * @author Julien Bonjean <julien@bonjean.info>
 *
 */
@SuppressWarnings("serial")
public class Filter implements Serializable {
	private final String field;
	private final String value;

	public Filter(String arg) throws ArgumentParserException {
		if (arg == null)
			throw new ArgumentParserException("null filter", null);

		if (!arg.contains("="))
			throw new ArgumentParserException("invalid filter format: " + arg, null);

		String[] parts = arg.split("=");

		if (parts.length != 2)
			throw new ArgumentParserException("invalid filter: " + arg, null);

		for (String part : parts) {
			part = part.trim();
			if (part.isEmpty())
				throw new ArgumentParserException("invalid filter: " + arg, null);
		}

		field = parts[0];
		value = parts[1];
	}

	public boolean match(JsonNode node) {
		// we assume our attributes (field, value) are clean (not null, not empty)

		if (node == null)
			return false;

		if (node.get(field) == null)
			return false;

		if (value.equals(node.get(field).textValue()))
			return true;

		return false;
	}

	public String getField() {
		return field;
	}

	public String getValue() {
		return value;
	}
}