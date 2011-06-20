package demonstrationServer.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

import coap.CodeRegistry;
import coap.LocalResource;
import coap.POSTRequest;

/*
 * This class implements a feedback resource for demonstration purposes.
 * 
 * Allows POST so send us an e-mail with a feedback (payload string)
 *  
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class FeedbackResource extends LocalResource {
	private List<String> feedbackReceivers;
	
	public FeedbackResource() {
		super("feedback");
		setResourceType("POST feedback using mail");
		
		//Spedify receiver list
		feedbackReceivers = new ArrayList<String>();
		feedbackReceivers.add("dapaulid@gmail.com");
		feedbackReceivers.add("dimobers@student.ethz.ch");
	}
	
	@Override
	public void performPOST(POSTRequest request) {

		// retrieve text to convert from payload
		String text = request.getPayloadString();
		
		sendFeedbackMail(text);

		// complete the request
		request.respond(CodeRegistry.V3_RESP_OK, "Sent: " + text.toUpperCase());
	}

	public void sendFeedbackMail(String messageText) {
		Properties properties = new Properties();
		properties.put("mail.smtp.host", "mail.nikix.ch");
		properties.put("mail.smtp.socketFactory.port", "465");
		properties.put("mail.smtp.socketFactory.class",
				"javax.net.ssl.SSLSocketFactory");
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.port", "465");
		Session session = Session.getInstance(properties,
				new javax.mail.Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication("californium@nikix.ch",
								"p@ssw0rd");
					}
				});
		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("californium@nikix.ch"));

			System.out.println("Sending mail to: ");

			Iterator<String> it = feedbackReceivers.iterator();
			while (it.hasNext()) {
				String receiver = it.next();
				message.addRecipients(Message.RecipientType.TO,
						InternetAddress.parse(receiver));
			}
			message.setSubject("[Californium] New Feedback");
			
			StringBuilder msgBuilder = new StringBuilder();
			msgBuilder.append("Dear Californium Team,\n\n");
			msgBuilder.append("I'd like to say the following about Californium:\n\n");
			msgBuilder.append(messageText);
			msgBuilder.append("\n\nThis is an e-mail sent by the demonstration server");
			message.setText(msgBuilder.toString());
			Transport.send(message);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

}
