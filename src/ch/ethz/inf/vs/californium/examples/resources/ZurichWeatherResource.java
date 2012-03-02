/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.examples.resources;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;


/*
 * This class implements a 'toUpper' resource for demonstration purposes.
 * 
 * Defines a resource that returns the current weather on a GET request.
 *  
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class ZurichWeatherResource extends LocalResource {

	// The current weather information represented as string
	private String weather;

	/*
	 * Constructor for a new ZurichWeatherResource
	 */
	public ZurichWeatherResource() {
		super("weatherResource");
		setResourceTitle("GET the current weather in zurich");
		setResourceType("ZurichWeather");
		// Set timer task scheduling
		// interval = 300'000 ms = 5 min
		Timer timer = new Timer();
		timer.schedule(new ZurichWeatherTask(), 0, 300000);
	}

	private class ZurichWeatherTask extends TimerTask {
		@Override
		public void run() {
			String newWeather = getZurichWeather("");
			if (!newWeather.equals(weather)) {
				weather = newWeather;
				changed();
			}
		}
	}

	@SuppressWarnings("deprecation")
	private String getZurichWeather(String format) {
		URL url;
		URLConnection urlConnection;
		DataInputStream dataStream;
		String rawWeather = "";
		String result = "";

		try {
			url = new URL("http://weather.yahooapis.com/forecastrss?w=12893366");
			urlConnection = url.openConnection();
			urlConnection.setDoInput(true);
			urlConnection.setUseCaches(false);

			dataStream = new DataInputStream(urlConnection.getInputStream());

			String current;
			while ((current = dataStream.readLine()) != null) {
				rawWeather += (current + "\n");
			}
			dataStream.close();
		} catch (IOException e) {
			System.err.println("getZurichWeather IOException");
			e.printStackTrace();
		}
		if (format.equals("plain")) {
			result = parseWeatherXML(rawWeather);
		} else {
			result = rawWeather;
		}
		return result;
	}

	/*
	 * Parses a given string describing an XML document
	 */
	public String parseWeatherXML(String input) {
		String result = "";
		try {
			Document xmlDocument = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder()
					.parse(new InputSource(new StringReader(input)));
			result = traverseXMLTree(xmlDocument);
		} catch (SAXException e) {
			System.err.println("parseWeatherXML SAXException");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("parseWeatherXML IOException");
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			System.err.println("parseWeatherXML ParserConfigurationException");
			e.printStackTrace();
		}
		return result;
	}

	/*
	 * Traverses the XML structure and parses relevant data
	 * 
	 * @param node The root node of the document
	 * 
	 * @return A formatted string containing the parsed data
	 */
	private String traverseXMLTree(Node node) {

		// Use stringbuilder to build result string more efficiently
		StringBuilder weatherResult = new StringBuilder();

		// Get location information
		if (node.getNodeName().equals("yweather:location")) {
			weatherResult.append("------------------------");
			weatherResult.append("\n  Location Information\n");
			weatherResult.append("------------------------");
			weatherResult.append("\n");
			// Get all location information attributes and append the
			// information to the string
			for (int j = 0; j < node.getAttributes().getLength(); j++) {
				weatherResult.append("   ");
				weatherResult
						.append(node.getAttributes().item(j).getNodeName());
				weatherResult.append(": ");
				weatherResult.append(node.getAttributes().item(j)
						.getNodeValue());
				weatherResult.append("\n");
			}
			weatherResult.append("------------------------\n\n");

			// Get unit information
		} else if (node.getNodeName().equals("yweather:units")) {
			weatherResult.append("------------------------");
			weatherResult.append("\n   Unit Information\n");
			weatherResult.append("------------------------");
			weatherResult.append("\n");
			// Get all unit information attributes and append the information
			// to the string
			for (int j = 0; j < node.getAttributes().getLength(); j++) {
				weatherResult.append("   ");
				weatherResult
						.append(node.getAttributes().item(j).getNodeName());
				weatherResult.append(": ");
				weatherResult.append(node.getAttributes().item(j)
						.getNodeValue());
				weatherResult.append("\n");
			}
			weatherResult.append("------------------------\n\n");

			// Get weather forecast information
		} else if (node.getNodeName().equals("yweather:forecast")) {
			weatherResult.append("-----------------------------");
			weatherResult.append("\n Weather Forecast: ");
			weatherResult.append(node.getAttributes().item(1).getNodeValue());
			weatherResult.append("\n");
			weatherResult.append("-----------------------------");
			weatherResult.append("\n");
			// Get all forecast information attributes and append the
			// information to the string. Start at attribute nr. 2 because
			// attribute 0 is the date of the forecast (already read for
			// constructing the header of the string) and attribute 1 is
			// not of interest
			for (int j = 2; j < node.getAttributes().getLength(); j++) {
				weatherResult.append("   ");
				weatherResult
						.append(node.getAttributes().item(j).getNodeName());
				weatherResult.append(": ");
				weatherResult.append(node.getAttributes().item(j)
						.getNodeValue());
				weatherResult.append("\n");
			}
			weatherResult.append("-----------------------------\n\n");
		}

		// Further traverse tree if children are available
		if (node.hasChildNodes()) {
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				weatherResult.append(traverseXMLTree(children.item(i)));
			}
		}
		return weatherResult.toString();
	}

	@Override
	public void performGET(GETRequest request) {

		// create response
		Response response = new Response(CodeRegistry.RESP_CONTENT);

		// Get Weather, either in plain text or as xml, depending on how it
		// has been requested
		if (request.getAccept()==MediaTypeRegistry.APPLICATION_XML) {
			weather = getZurichWeather("xml");
		} else if (request.getAccept()==MediaTypeRegistry.TEXT_PLAIN || request.getAccept()==-1) {
			weather = getZurichWeather("plain");
		} else {
			request.respond(CodeRegistry.RESP_NOT_ACCEPTABLE);
			return;
		}

		// set payload
		response.setPayload(weather);

		// complete the request
		request.respond(response);
	}
}
